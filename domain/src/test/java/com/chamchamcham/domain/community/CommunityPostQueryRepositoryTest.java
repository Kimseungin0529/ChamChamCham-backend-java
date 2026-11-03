package com.chamchamcham.domain.community;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(CommunityPostQueryRepositoryImpl.class)
class CommunityPostQueryRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private CommunityPostQueryRepository queryRepository;
    private Member member;
    private Member otherMember;
    private Crop hwanggiCrop;
    private Crop ginsengCrop;
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 12, 7, 0);
    private UUID memberId() { return Objects.requireNonNull(member.getId()); }
    private UUID hwanggiCropId() { return Objects.requireNonNull(hwanggiCrop.getId()); }
    private UUID ginsengCropId() { return Objects.requireNonNull(ginsengCrop.getId()); }
    @BeforeEach
    void setUp() {
        member = persist(member("member@example.com"), now);
        otherMember = persist(member("other@example.com"), now);
        hwanggiCrop = persist(new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK), now);
        ginsengCrop = persist(new Crop(null, 107, "인삼", CropUsePartCategory.ROOT_BARK), now);
    }

    @Test
    void searchReturnsActivePostsLatestFirstWithCursor() {
        persistPost("최신 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 10, 0), false, 0, 0, 0);
        persistPost("중간 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("오래된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 8, 0), false, 0, 0, 0);
        persistPost("삭제된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 11, 0), true, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var firstPage = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 2));
        var secondPage = queryRepository.search(
            condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LATEST, new CommunityPostQueryRepository.Cursor(CommunityPostSort.LATEST, null, firstPage.rows().getLast().post().getCreatedAt(), java.util.Objects.requireNonNull(firstPage.rows().getLast().post().getId())), 2)
        );

        assertThat(firstPage.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("최신 글", "중간 글");
        assertThat(firstPage.rows().stream().map(it -> it.post().getTitle()).toList()).doesNotContain("삭제된 글");
        assertThat(secondPage.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("오래된 글");

    }

    @Test
    void searchFiltersByMultipleCropsPostTypeAndKeyword() {
        persistPost("황기 발아율 질문", "발아가 안 됩니다", otherMember, hwanggiCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("인삼 발아율 질문", "발아가 안 됩니다", otherMember, ginsengCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("황기 자유글", "발아 얘기", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(
            condition(memberId(), null, null, List.of(hwanggiCropId(), ginsengCropId()), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, 20)
        );

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList())
            .containsExactlyInAnyOrder("황기 발아율 질문", "인삼 발아율 질문");
        assertThat(result.rows().stream().allMatch(it -> List.of(hwanggiCropId(), ginsengCropId()).contains(it.post().getCrop().getId()))).isTrue();
        assertThat(result.rows().stream().allMatch(it -> it.post().getPostType() == CommunityPostType.QUESTION)).isTrue();

    }

    @Test
    void searchFiltersLikedonlyAndMineonly() {
        var likedPost = persistPost("내가 좋아요한 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("내 글", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persist(new CommunityPostLike(null, likedPost, member), LocalDateTime.of(2026, 6, 12, 10, 10));
        entityManager.flush();
        entityManager.clear();

        var liked = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, true, false, CommunityPostSort.LATEST, null, 20));
        var mine = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, true, CommunityPostSort.LATEST, null, 20));

        assertThat(liked.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("내가 좋아요한 글");
        assertThat(liked.rows().getFirst().likedByMe()).isTrue();
        assertThat(mine.rows().stream().map(it -> it.post().getAuthor().getId()).toList()).containsOnly(memberId());

    }

    @Test
    void searchFiltersByAuthorMemberIdWithoutChangingViewerLikeState() {
        var targetPost = persistPost("다른 회원 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("내 글", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persist(new CommunityPostLike(null, targetPost, member), LocalDateTime.of(2026, 6, 12, 10, 10));
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(
            condition(memberId(), java.util.Objects.requireNonNull(otherMember.getId()), null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20)
        );

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("다른 회원 글");
        assertThat(result.rows().getFirst().likedByMe()).isTrue();

    }

    @Test
    void searchReturnsPostsForGuestWithLikedbymeFalse() {
        persistPost("비회원 공개 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(null, null, null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20));

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("비회원 공개 글");
        assertThat(result.rows().getFirst().likedByMe()).isFalse();

    }

    @Test
    void searchSortsByLikeCountThenLatest() {
        persistPost("좋아요 둘 오래된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 8, 0), false, 0, 0, 2);
        persistPost("좋아요 하나 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 10, 0), false, 0, 0, 1);
        persistPost("좋아요 둘 최신 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 2);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LIKE, null, 20));

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("좋아요 둘 최신 글", "좋아요 둘 오래된 글", "좋아요 하나 글");
        assertThat(result.rows().stream().map(it -> it.score()).toList()).containsExactly(2L, 2L, 1L);
        assertThat(result.rows().stream().map(it -> it.likeCount()).toList()).containsExactly(2L, 2L, 1L);

    }

    @Test
    void searchSortsByNonDeletedCommentCountThenLatest() {
        persistPost("댓글 둘 오래된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 8, 0), false, 2, 0, 0);
        persistPost("댓글 하나와 삭제 댓글 많은 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 10, 0), false, 1, 3, 0);
        persistPost("댓글 둘 최신 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 2, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.COMMENT, null, 20));

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("댓글 둘 최신 글", "댓글 둘 오래된 글", "댓글 하나와 삭제 댓글 많은 글");
        assertThat(result.rows().stream().map(it -> it.score()).toList()).containsExactly(2L, 2L, 1L);
        assertThat(result.rows().stream().map(it -> it.commentCount()).toList()).containsExactly(2L, 2L, 1L);

    }

    @Test
    void searchSortsByPopularScoreThenLatest() {
        persistPost("인기 셋 오래된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 8, 0), false, 2, 0, 1);
        persistPost("인기 둘 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 10, 0), false, 1, 0, 1);
        persistPost("인기 셋 최신 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 3);
        entityManager.flush();
        entityManager.clear();

        var result = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.POPULAR, null, 20));

        assertThat(result.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("인기 셋 최신 글", "인기 셋 오래된 글", "인기 둘 글");
        assertThat(result.rows().stream().map(it -> it.score()).toList()).containsExactly(3L, 3L, 2L);
        assertThat(result.rows().stream().map(it -> it.likeCount() + it.commentCount()).toList()).containsExactly(3L, 3L, 2L);

    }

    @Test
    void searchUsesCountBasedCursorPagination() {
        persistPost("좋아요 다섯 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 11, 0), false, 0, 0, 5);
        persistPost("좋아요 넷 최신 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 10, 0), false, 0, 0, 4);
        persistPost("좋아요 넷 오래된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 4);
        persistPost("좋아요 셋 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 8, 0), false, 0, 0, 3);
        entityManager.flush();
        entityManager.clear();

        var firstPage = queryRepository.search(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LIKE, null, 2));
        var cursorRow = firstPage.rows().getLast();
        var secondPage = queryRepository.search(
            condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LIKE, new CommunityPostQueryRepository.Cursor(CommunityPostSort.LIKE, cursorRow.score(), cursorRow.post().getCreatedAt(), java.util.Objects.requireNonNull(cursorRow.post().getId())), 2)
        );

        assertThat(firstPage.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("좋아요 다섯 글", "좋아요 넷 최신 글");
        assertThat(secondPage.rows().stream().map(it -> it.post().getTitle()).toList()).containsExactly("좋아요 넷 오래된 글", "좋아요 셋 글");

    }

    @Test
    void searchConditionSupportsSortAndStructuredCursor() {
        var cursor = new CommunityPostQueryRepository.Cursor(CommunityPostSort.LIKE, 3L, LocalDateTime.of(2026, 6, 12, 9, 0), UUID.fromString("00000000-0000-0000-0000-000000000101"));

        var condition = condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LIKE, cursor, 20);

        assertThat(condition.sort()).isEqualTo(CommunityPostSort.LIKE);
        assertThat(condition.cursor()).isEqualTo(cursor);

    }

    @Test
    void countReturnsTotalMatchingPostsIgnoringCursorAndSize() {
        persistPost("황기 발아율 질문", "본문", otherMember, hwanggiCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("황기 자유글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("인삼 발아율 질문", "본문", otherMember, ginsengCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(memberId(), null, hwanggiCropId(), List.of(hwanggiCropId()), null, null, false, false, CommunityPostSort.LATEST, null, 1));

        assertThat(total).isEqualTo(2);

    }

    @Test
    void countAppliesKeywordPostTypeAndMineonlyLikedonlyFiltersSameAsSearch() {
        var likedPost = persistPost("황기 발아율 질문", "발아가 안 됩니다", otherMember, hwanggiCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("인삼 발아율 질문", "발아가 안 됩니다", otherMember, ginsengCrop, CommunityPostType.QUESTION, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persist(new CommunityPostLike(null, likedPost, member), LocalDateTime.of(2026, 6, 12, 10, 10));
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(memberId(), null, hwanggiCropId(), List.of(hwanggiCropId()), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, 20));
        var likedTotal = queryRepository.count(condition(memberId(), null, null, List.of(), null, null, true, false, CommunityPostSort.LATEST, null, 20));

        assertThat(total).isEqualTo(1);
        assertThat(likedTotal).isEqualTo(1);

    }

    @Test
    void countExcludesDeletedPosts() {
        persistPost("정상 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("삭제된 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), true, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(condition(memberId(), null, null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20));

        assertThat(total).isEqualTo(1);

    }

    @Test
    void countFiltersByAuthorMemberId() {
        persistPost("다른 회원 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("내 글", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(
            condition(memberId(), java.util.Objects.requireNonNull(otherMember.getId()), null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20)
        );

        assertThat(total).isEqualTo(1);

    }

    @Test
    void countPreservesAuthorFilterForGuest() {
        persistPost("다른 회원 글", "본문", otherMember, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("내 글", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var total = queryRepository.count(
            condition(null, java.util.Objects.requireNonNull(otherMember.getId()), null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20)
        );

        assertThat(total).isEqualTo(1);

    }

    @Test
    void findDistinctCropsByAuthorDedupesCropsAndExcludesDeletedPostsAndOtherAuthors() {
        persistPost("황기 글 1", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("황기 글 2", "본문", member, hwanggiCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        persistPost("삭제된 인삼 글", "본문", member, ginsengCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), true, 0, 0, 0);
        persistPost("다른 회원 인삼 글", "본문", otherMember, ginsengCrop, CommunityPostType.GENERAL, LocalDateTime.of(2026, 6, 12, 9, 0), false, 0, 0, 0);
        entityManager.flush();
        entityManager.clear();

        var crops = queryRepository.findDistinctCropsByAuthor(memberId());

        assertThat(crops.stream().map(it -> it.getId()).toList()).containsExactly(hwanggiCropId());

    }
    @Test
    void cursorUsesIdWhenTimestampsAndScoresTieForEverySort() {
        LocalDateTime at = LocalDateTime.of(2026, 6, 12, 9, 0);
        for (String title : List.of("one", "two", "three")) {
            persistPost(title, "body", otherMember, hwanggiCrop, CommunityPostType.GENERAL, at, false, 1, 0, 1);
        }
        entityManager.clear();
        for (CommunityPostSort sort : CommunityPostSort.values()) {
            var all = queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                    memberId(), null, List.of(), null, null, false, false, sort, null, 20)).rows();
            var first = all.getFirst();
            var cursor = new CommunityPostQueryRepository.Cursor(sort, first.score(), first.post().getCreatedAt(), first.post().getId());
            var after = queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                    memberId(), null, List.of(), null, null, false, false, sort, cursor, 1)).rows();
            assertThat(after).hasSize(1);
            assertThat(after.getFirst().post().getId()).isEqualTo(all.get(1).post().getId());
            assertThat(queryRepository.count(new CommunityPostQueryRepository.SearchCondition(
                    memberId(), null, List.of(), null, null, false, false, sort, cursor, 1))).isEqualTo(3);
        }
    }

    @Test
    void keywordTrimsUnicodeWhitespaceAndIgnoresCase() {
        persistPost("Seed question", "body", otherMember, hwanggiCrop, CommunityPostType.QUESTION,
                now, false, 0, 0, 0);
        var result = queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                null, null, List.of(), null, "\u00a0SEED\u00a0", false, false, CommunityPostSort.LATEST, null, 20));
        assertThat(result.rows()).extracting(row -> row.post().getTitle()).containsExactly("Seed question");
    }

    @Test
    void guestPersonalFiltersAndMissingCountCursorScoreKeepDomainExceptions() {
        assertThatThrownBy(() -> queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                null, null, List.of(), null, null, true, false, CommunityPostSort.LATEST, null, 20)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Member id is required for liked-only search");
        assertThatThrownBy(() -> queryRepository.count(new CommunityPostQueryRepository.SearchCondition(
                null, null, List.of(), null, null, false, true, CommunityPostSort.LATEST, null, 20)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Member id is required for mine-only search");
        var cursor = new CommunityPostQueryRepository.Cursor(CommunityPostSort.LIKE, null, now, UUID.randomUUID());
        assertThatThrownBy(() -> queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                memberId(), null, List.of(), null, null, false, false, CommunityPostSort.LIKE, cursor, 20)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Cursor score is required for LIKE sort");
    }

    @Test
    void thumbnailComesFromLowestDisplayOrder() {
        var post = persistPost("thumbnail", "body", otherMember, hwanggiCrop, CommunityPostType.GENERAL,
                now, false, 0, 0, 0);
        var media = persist(new UploadedMedia(null, otherMember, UploadedMediaType.IMAGE,
                UploadedMediaUsageType.COMMUNITY_POST, "https://example.test/first.jpg", "community/first",
                UploadedMediaStatus.ATTACHED), now);
        persist(new CommunityPostMedia(post, media, -1), now);
        entityManager.clear();
        var result = queryRepository.search(new CommunityPostQueryRepository.SearchCondition(
                memberId(), null, List.of(), null, null, false, false, CommunityPostSort.LATEST, null, 20));
        assertThat(result.rows().getFirst().thumbnailUrl()).isEqualTo("https://example.test/first.jpg");
    }

    private Member member(String email) {
        return new Member(null, email, null, com.chamchamcham.domain.member.MemberStatus.ACTIVE,
                null, null, null, null, null, null, "ROLE_USER", null, null);
    }
    private void setTimestamps(BaseTimeEntity entity, LocalDateTime at) {
        try {
            var created = BaseTimeEntity.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(entity, at);
            var updated = BaseTimeEntity.class.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(entity, at);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
    private <T extends BaseTimeEntity> T persist(T entity, LocalDateTime at) {
        setTimestamps(entity, at);
        return entityManager.persistAndFlush(entity);
    }
    private CommunityPost persistPost(String title, String body, Member author, Crop crop, CommunityPostType type,
                                      LocalDateTime at, boolean deleted, int comments, int deletedComments, int likes) {
        CommunityPost post = persist(new CommunityPost(null, author, crop, null, type, title, body, deleted), at);
        for (int i = 0; i < comments; i++) persistComment(post, false);
        for (int i = 0; i < deletedComments; i++) persistComment(post, true);
        for (int i = 0; i < likes; i++) {
            Member liker = persist(member("like-" + UUID.randomUUID() + "@example.com"), now);
            persist(new CommunityPostLike(post, liker), LocalDateTime.of(2026, 6, 12, 9, 40));
        }
        UploadedMedia media = persist(new UploadedMedia(null, otherMember, UploadedMediaType.IMAGE,
                UploadedMediaUsageType.COMMUNITY_POST, "https://example.test/" + title + ".jpg", "community/" + title,
                UploadedMediaStatus.ATTACHED), LocalDateTime.of(2026, 6, 12, 9, 50));
        persist(new CommunityPostMedia(post, media, 0), LocalDateTime.of(2026, 6, 12, 9, 51));
        return post;
    }
    private void persistComment(CommunityPost post, boolean deleted) {
        persist(new CommunityComment(null, post, null, member, "댓글", null, deleted), LocalDateTime.of(2026, 6, 12, 9, 30));
    }
    private CommunityPostQueryRepository.SearchCondition condition(UUID memberId, UUID authorId, UUID cropId,
            List<UUID> cropIds, CommunityPostType type, String keyword, boolean liked, boolean mine,
            CommunityPostSort sort, CommunityPostQueryRepository.Cursor cursor, int size) {
        return new CommunityPostQueryRepository.SearchCondition(memberId, authorId, cropIds, type, keyword,
                liked, mine, sort, cursor, size);
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.chamchamcham.domain")
@EnableJpaRepositories(basePackages = "com.chamchamcham.domain")
class CommunityPostQueryRepositoryTestApplication {}
