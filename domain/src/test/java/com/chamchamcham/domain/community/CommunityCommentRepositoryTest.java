package com.chamchamcham.domain.community;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.member.Member;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CommunityCommentRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private CommunityCommentRepository repository;

    @Test
    void findRootPageOrdersLatestFirstAndAppliesCursor() {
        var fixture = persistPostFixture();
        var older = persistComment(fixture.post(), fixture.member(), "older", null, LocalDateTime.of(2026, 6, 12, 9, 0));
        var middle = persistComment(fixture.post(), fixture.member(), "middle", null, LocalDateTime.of(2026, 6, 12, 10, 0));
        var newest = persistComment(fixture.post(), fixture.member(), "newest", null, LocalDateTime.of(2026, 6, 12, 11, 0));
        persistComment(fixture.post(), fixture.member(), "reply", newest, LocalDateTime.of(2026, 6, 12, 11, 1));
        entityManager.flush();
        entityManager.clear();

        var firstPage = repository.findRootFirstPage(
            java.util.Objects.requireNonNull(fixture.post().getId()),
            PageRequest.of(0, 2)
        );
        var secondPage = repository.findRootPageAfter(
            java.util.Objects.requireNonNull(fixture.post().getId()),
            firstPage.getLast().getCreatedAt(),
            java.util.Objects.requireNonNull(firstPage.getLast().getId()),
            PageRequest.of(0, 2)
        );

        assertThat(firstPage.stream().map(it -> it.getBody()).toList()).containsExactly("newest", "middle");
        assertThat(firstPage).allMatch(it -> it.getParentComment() == null);
        assertThat(secondPage.stream().map(it -> it.getBody()).toList()).containsExactly("older");
        assertThat(older.getId()).isNotNull();
        assertThat(middle.getId()).isNotNull();

    }

    @Test
    void findRepliesReturnsOnlyRequestedParentsOldestFirst() {
        var fixture = persistPostFixture();
        var root1 = persistComment(fixture.post(), fixture.member(), "root1", null, LocalDateTime.of(2026, 6, 12, 9, 0));
        var root2 = persistComment(fixture.post(), fixture.member(), "root2", null, LocalDateTime.of(2026, 6, 12, 9, 1));
        persistComment(fixture.post(), fixture.member(), "root1 reply 2", root1, LocalDateTime.of(2026, 6, 12, 9, 3));
        persistComment(fixture.post(), fixture.member(), "root1 reply 1", root1, LocalDateTime.of(2026, 6, 12, 9, 2));
        persistComment(fixture.post(), fixture.member(), "root2 reply", root2, LocalDateTime.of(2026, 6, 12, 9, 4));
        entityManager.flush();
        entityManager.clear();

        var replies = repository.findRepliesByParentIds(List.of(java.util.Objects.requireNonNull(root1.getId())));

        assertThat(replies.stream().map(it -> it.getBody()).toList()).containsExactly("root1 reply 1", "root1 reply 2");
        assertThat(replies).allMatch(it -> it.getParentComment().getId().equals(root1.getId()));
        assertThat(root2.getId()).isNotNull();

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
    private Fixture persistPostFixture() {
        LocalDateTime at = LocalDateTime.of(2026, 6, 12, 8, 0);
        Member member = persist(member("member-" + UUID.randomUUID() + "@example.com"), at);
        Crop crop = persist(new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK), at);
        CommunityPost post = persist(new CommunityPost(member, crop, CommunityPostType.QUESTION, "황기 발아율", "싹이 거의 올라오지 않아요."), at);
        return new Fixture(member, post);
    }
    private CommunityComment persistComment(
            CommunityPost post,
            Member member,
            String body,
            CommunityComment parent,
            LocalDateTime at
    ) {
        return persist(new CommunityComment(null, post, parent, member, body, null, false), at);
    }
    private record Fixture(Member member, CommunityPost post) {}
}
