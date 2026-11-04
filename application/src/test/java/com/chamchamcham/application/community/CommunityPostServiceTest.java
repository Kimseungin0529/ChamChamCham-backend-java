package com.chamchamcham.application.community;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.community.CommunityCommentRepository;
import com.chamchamcham.domain.community.CommunityPost;
import com.chamchamcham.domain.community.CommunityPostLike;
import com.chamchamcham.domain.community.CommunityPostLikeRepository;
import com.chamchamcham.domain.community.CommunityPostMedia;
import com.chamchamcham.domain.community.CommunityPostMediaRepository;
import com.chamchamcham.domain.community.CommunityPostQueryRepository;
import com.chamchamcham.domain.community.CommunityPostRepository;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.HarvestSource;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID secondPostId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID secondCropId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID mediaId1 = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final UUID mediaId2 = UUID.fromString("00000000-0000-0000-0000-000000000402");
    private final UUID replacementMediaId = UUID.fromString("00000000-0000-0000-0000-000000000403");
    private final LocalDateTime postCreatedAt = LocalDateTime.of(2026, 6, 12, 9, 0);
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    @Mock private MemberRepository memberRepository;
    @Mock private CropRepository cropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    @Mock private CommunityPostRepository communityPostRepository;
    @Mock private CommunityPostMediaRepository communityPostMediaRepository;
    @Mock private CommunityCommentRepository communityCommentRepository;
    @Mock private CommunityPostLikeRepository communityPostLikeRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private CommunityPostQueryRepository communityPostQueryRepository;
    private CommunityPostService service;
    private Member member;
    private Member otherMember;
    private Crop crop;
    private Crop secondCrop;
    private FarmingRecord record;
    private UploadedMedia media1;
    private UploadedMedia media2;
    private UploadedMedia replacementMedia;
    private CommunityPost existingPost;
    private CommunityPostLike existingLike;

    @BeforeEach
    void setUp() throws Exception {
        member = member(memberId);
        member.setNickname("황기농부");
        otherMember = member(otherMemberId);
        crop = crop(cropId, "황기");
        secondCrop = crop(secondCropId, "인삼");
        record = farmingRecord(member, crop);
        media1 = uploadedMedia(member, mediaId1, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        media2 = uploadedMedia(member, mediaId2, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        replacementMedia = uploadedMedia(member, replacementMediaId, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        existingPost = existingPost(member, crop, postId);
        existingLike = new CommunityPostLike(UUID.randomUUID(), existingPost, member);
        service = new CommunityPostService(memberRepository, cropRepository, farmingRecordRepository, uploadedMediaRepository, communityPostRepository, communityPostMediaRepository, communityCommentRepository, communityPostLikeRepository, memberCropRepository, communityPostQueryRepository, cursorCodec);
    }

    @Test
    @DisplayName("create stores post with crop farming record and attached images")
    void create_stores_post_with_crop_farming_record_and_attached_images() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(record);
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1, mediaId2))).thenReturn(java.util.Arrays.asList(media1, media2));
        when(communityPostRepository.save(any(CommunityPost.class))).thenAnswer(invocation -> {
        var post = invocation.<CommunityPost>getArgument(0);
        return new CommunityPost(postId, post.getAuthor(), post.getCrop(), post.getFarmingRecord(), post.getPostType(), post.getTitle(), post.getBody(), false);
        });
        var result = service.create(createCommand(java.util.Arrays.asList(mediaId1, mediaId2), recordId));
        assertEquals(postId, result.getId());
        var savedPost = capturedPost();
        assertEquals(memberId, savedPost.getAuthor().getId());
        assertEquals(cropId, savedPost.getCrop().getId());
        assertEquals(recordId, savedPost.getFarmingRecord().getId());
        assertEquals(CommunityPostType.QUESTION, savedPost.getPostType());
        assertEquals("황기 발아율이 낮아요", savedPost.getTitle());
        assertEquals("싹이 거의 올라오지 않아요.", savedPost.getBody());
        assertEquals(UploadedMediaStatus.ATTACHED, media1.getStatus());
        assertEquals(UploadedMediaStatus.ATTACHED, media2.getStatus());
        assertThat(capturedPostMedia().stream().map(it -> it.getDisplayOrder()).toList()).containsExactly(0, 1);
    }

    @Test
    @DisplayName("create rejects farming record with different crop")
    void create_rejects_farming_record_with_different_crop() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findById(cropId)).thenReturn(Optional.of(crop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(farmingRecord(member, secondCrop));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(List.of(), recordId)));
        assertEquals(ErrorCode.COMMUNITY_FARMING_RECORD_CROP_MISMATCH, exception.getErrorCode());
        verify(communityPostRepository, never()).save(any(CommunityPost.class));
    }

    @Test
    @DisplayName("update allows author to change crop farming record content and images")
    void update_allows_author_to_change_crop_farming_record_content_and_images() throws Exception {
        var secondRecord = farmingRecord(member, secondCrop);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(cropRepository.findById(secondCropId)).thenReturn(Optional.of(secondCrop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(secondRecord);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(List.of());
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(replacementMediaId))).thenReturn(java.util.Arrays.asList(replacementMedia));
        when(communityPostMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(replacementMediaId))).thenReturn(List.of());
        var result = service.update(updateCommand(java.util.Arrays.asList(replacementMediaId), recordId));
        assertEquals(postId, result.getId());
        assertEquals(secondCropId, existingPost.getCrop().getId());
        assertEquals(CommunityPostType.GENERAL, existingPost.getPostType());
        assertEquals("수정된 제목", existingPost.getTitle());
        assertEquals("수정된 본문", existingPost.getBody());
        assertEquals(recordId, existingPost.getFarmingRecord().getId());
        assertEquals(UploadedMediaStatus.ATTACHED, replacementMedia.getStatus());
    }

    @Test
    @DisplayName("update keeps existing post image and attaches new image in requested order")
    void update_keeps_existing_post_image_and_attaches_new_image_in_requested_order() throws Exception {
        var secondRecord = farmingRecord(member, secondCrop);
        var existingMedia = uploadedMedia(member, mediaId1, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.ATTACHED);
        var newMedia = uploadedMedia(member, replacementMediaId, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        var existingPostMedia = new CommunityPostMedia(existingPost, existingMedia, 0);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(cropRepository.findById(secondCropId)).thenReturn(Optional.of(secondCrop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(secondRecord);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(java.util.Arrays.asList(existingPostMedia));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1, replacementMediaId))).thenReturn(java.util.Arrays.asList(existingMedia, newMedia));
        when(communityPostMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1, replacementMediaId))).thenReturn(java.util.Arrays.asList(existingPostMedia));
        service.update(updateCommand(java.util.Arrays.asList(mediaId1, replacementMediaId), recordId));
        assertEquals(UploadedMediaStatus.ATTACHED, existingMedia.getStatus());
        assertEquals(UploadedMediaStatus.ATTACHED, newMedia.getStatus());
        var postMedia = capturedPostMedia();
        assertThat(postMedia.stream().map(it -> it.getUploadedMedia().getId()).toList()).containsExactly(mediaId1, replacementMediaId);
        assertThat(postMedia.stream().map(it -> it.getDisplayOrder()).toList()).containsExactly(0, 1);
    }

    @Test
    @DisplayName("update marks removed existing image as deleted")
    void update_marks_removed_existing_image_as_deleted() throws Exception {
        var secondRecord = farmingRecord(member, secondCrop);
        var keptMedia = uploadedMedia(member, mediaId1, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.ATTACHED);
        var removedMedia = uploadedMedia(member, mediaId2, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.ATTACHED);
        var keptPostMedia = new CommunityPostMedia(existingPost, keptMedia, 0);
        var removedPostMedia = new CommunityPostMedia(existingPost, removedMedia, 1);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(cropRepository.findById(secondCropId)).thenReturn(Optional.of(secondCrop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(secondRecord);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(java.util.Arrays.asList(keptPostMedia, removedPostMedia));
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(keptMedia));
        when(communityPostMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(keptPostMedia));
        service.update(updateCommand(java.util.Arrays.asList(mediaId1), recordId));
        assertEquals(UploadedMediaStatus.ATTACHED, keptMedia.getStatus());
        assertEquals(UploadedMediaStatus.DELETED, removedMedia.getStatus());
        assertThat(capturedPostMedia().stream().map(it -> it.getUploadedMedia().getId()).toList()).containsExactly(mediaId1);
    }

    @Test
    @DisplayName("update rejects image already attached to another post")
    void update_rejects_image_already_attached_to_another_post() throws Exception {
        var secondRecord = farmingRecord(member, secondCrop);
        var otherPost = existingPost(member, crop, secondPostId);
        var otherPostMedia = uploadedMedia(member, mediaId1, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.ATTACHED);
        var otherPostMediaRow = new CommunityPostMedia(otherPost, otherPostMedia, 0);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(cropRepository.findById(secondCropId)).thenReturn(Optional.of(secondCrop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(secondRecord);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(List.of());
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(otherPostMedia));
        when(communityPostMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(otherPostMediaRow));
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(java.util.Arrays.asList(mediaId1), recordId)));
        assertEquals(ErrorCode.MEDIA_NOT_ATTACHABLE, exception.getErrorCode());
        verify(communityPostMediaRepository, never()).deleteByPost(existingPost);
    }

    @Test
    @DisplayName("update rejects another member image")
    void update_rejects_another_member_image() throws Exception {
        var secondRecord = farmingRecord(member, secondCrop);
        var otherMemberMedia = uploadedMedia(otherMember, mediaId1, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(cropRepository.findById(secondCropId)).thenReturn(Optional.of(secondCrop));
        when(farmingRecordRepository.findByIdAndMember_Id(recordId, memberId)).thenReturn(secondRecord);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(List.of());
        when(uploadedMediaRepository.findAllById(java.util.Arrays.asList(mediaId1))).thenReturn(java.util.Arrays.asList(otherMemberMedia));
        when(communityPostMediaRepository.findByUploadedMediaIdIn(java.util.Arrays.asList(mediaId1))).thenReturn(List.of());
        var exception = assertThrows(BusinessException.class, () -> service.update(updateCommand(java.util.Arrays.asList(mediaId1), recordId)));
        assertEquals(ErrorCode.MEDIA_NOT_OWNED, exception.getErrorCode());
        verify(communityPostMediaRepository, never()).deleteByPost(existingPost);
    }

    @Test
    @DisplayName("delete rejects non author")
    void delete_rejects_non_author() throws Exception {
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        var exception = assertThrows(BusinessException.class, () -> service.delete(new CommunityPostCommand.Delete(otherMemberId, postId)));
        assertEquals(ErrorCode.COMMUNITY_FORBIDDEN, exception.getErrorCode());
        assertFalse(existingPost.isDeleted());
    }

    @Test
    @DisplayName("toggle like creates then removes like row")
    void toggle_like_creates_then_removes_like_row() throws Exception {
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(communityPostLikeRepository.findByPostIdAndMemberId(postId, memberId)).thenReturn(null).thenReturn(existingLike);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(communityPostLikeRepository.countByPostId(postId)).thenReturn(1L).thenReturn(0L);
        var first = service.toggleLike(new CommunityPostCommand.ToggleLike(memberId, postId));
        var second = service.toggleLike(new CommunityPostCommand.ToggleLike(memberId, postId));
        assertTrue(first.getLiked());
        assertEquals(1L, first.getLikeCount());
        verify(communityPostLikeRepository).save(any(CommunityPostLike.class));
        assertFalse(second.getLiked());
        assertEquals(0L, second.getLikeCount());
        verify(communityPostLikeRepository).delete(existingLike);
    }

    @Test
    @DisplayName("list boards de duplicates member crops by crop id preserving first encounter order")
    void list_boards_de_duplicates_member_crops_by_crop_id_preserving_first_encounter_order() throws Exception {
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(java.util.Arrays.asList(memberCrop(member, crop), memberCrop(member, crop), memberCrop(member, secondCrop)));
        var boards = service.listBoards(memberId);
        assertThat(boards.stream().map(it -> it.getCropId()).toList()).containsExactly(cropId, secondCropId);
        assertThat(boards.stream().map(it -> it.getCropName()).toList()).containsExactly("황기", "인삼");
    }

    @Test
    @DisplayName("list post crops maps distinct crops from the query repository")
    void list_post_crops_maps_distinct_crops_from_the_query_repository() throws Exception {
        when(communityPostQueryRepository.findDistinctCropsByAuthor(memberId)).thenReturn(java.util.Arrays.asList(crop, secondCrop));
        var boards = service.listPostCrops(memberId);
        assertThat(boards.stream().map(it -> it.getCropId()).toList()).containsExactly(cropId, secondCropId);
        assertThat(boards.stream().map(it -> it.getCropName()).toList()).containsExactly("황기", "인삼");
    }

    @Test
    @DisplayName("get detail returns ordered images counts and liked by me")
    void get_detail_returns_ordered_images_counts_and_liked_by_me() throws Exception {
        setCreatedAt(existingPost, postCreatedAt);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(java.util.Arrays.asList(new CommunityPostMedia(existingPost, media1, 0), new CommunityPostMedia(existingPost, media2, 1)));
        when(communityCommentRepository.countByPostIdAndIsDeletedFalse(postId)).thenReturn(3L);
        when(communityPostLikeRepository.countByPostId(postId)).thenReturn(8L);
        when(communityPostLikeRepository.existsByPostIdAndMemberId(postId, memberId)).thenReturn(true);
        var detail = service.getDetail(memberId, postId);
        assertEquals(postId, detail.getId());
        assertThat(detail.getImageUrls()).containsExactly("https://example.test/1.jpg", "https://example.test/2.jpg");
        assertEquals(3L, detail.getCommentCount());
        assertEquals(8L, detail.getLikeCount());
        assertTrue(detail.getLikedByMe());
    }

    @Test
    @DisplayName("guest detail returns false without like lookup")
    void guest_detail_returns_false_without_like_lookup() throws Exception {
        setCreatedAt(existingPost, postCreatedAt);
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(existingPost);
        when(communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)).thenReturn(List.of());
        when(communityCommentRepository.countByPostIdAndIsDeletedFalse(postId)).thenReturn(0L);
        when(communityPostLikeRepository.countByPostId(postId)).thenReturn(0L);
        var detail = service.getDetail(null, postId);
        assertFalse(detail.getLikedByMe());
        verify(communityPostLikeRepository, never()).existsByPostIdAndMemberId(postId, memberId);
    }

    @Test
    @DisplayName("search maps application condition to query repository and next cursor")
    void search_maps_application_condition_to_query_repository_and_next_cursor() throws Exception {
        var requestedSize = 1;
        var overflowPost = existingPost(member, crop, secondPostId);
        setCreatedAt(overflowPost, postCreatedAt.minusMinutes(1));
        setCreatedAt(existingPost, postCreatedAt);
        when(communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(memberId, otherMemberId, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, requestedSize+ 1))).thenReturn(new CommunityPostQueryRepository.SearchResult(java.util.Arrays.asList(queryRow(existingPost, "https://example.test/1.jpg", 3, 8, true, null), queryRow(overflowPost, "https://example.test/2.jpg", 3, 8, true, null))));
        var page = service.search(new CommunityPostSearchCondition(memberId, otherMemberId, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, requestedSize));
        assertThat(page.getItems()).hasSize(1);
        assertEquals(postId, page.getItems().get(0).getId());
        assertThat(page.getNextCursor()).isNotBlank();
        var nextCursor = cursorCodec.decode(page.getNextCursor(), CommunityPostCursorPayload.class);
        assertEquals(CommunityPostSort.LATEST, nextCursor.getSort());
        assertEquals(null, nextCursor.getScore());
        assertEquals(postCreatedAt, nextCursor.getCreatedAt());
        assertEquals(postId, nextCursor.getId());
    }

    @Test
    @DisplayName("guest search delegates without viewer personalization")
    void guest_search_delegates_without_viewer_personalization() throws Exception {
        when(communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(null, otherMemberId, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, 21))).thenReturn(new CommunityPostQueryRepository.SearchResult(List.of()));
        var page = service.search(searchCondition(null, otherMemberId, false, false, CommunityPostSort.LATEST, null, 20));
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("guest search rejects liked only before repository access")
    void guest_search_rejects_liked_only_before_repository_access() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(null, null, true, false, CommunityPostSort.LATEST, null, 20)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("guest search rejects mine only before repository access")
    void guest_search_rejects_mine_only_before_repository_access() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(null, null, false, true, CommunityPostSort.LATEST, null, 20)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search encodes count based next cursor with score")
    void search_encodes_count_based_next_cursor_with_score() throws Exception {
        var requestedSize = 1;
        var overflowPost = existingPost(member, crop, secondPostId);
        setCreatedAt(overflowPost, postCreatedAt.minusMinutes(1));
        setCreatedAt(existingPost, postCreatedAt);
        when(communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(memberId, null, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LIKE, null, requestedSize+ 1))).thenReturn(new CommunityPostQueryRepository.SearchResult(java.util.Arrays.asList(queryRow(existingPost, "https://example.test/1.jpg", 3, 8, true, 8L), queryRow(overflowPost, "https://example.test/2.jpg", 3, 8, true, 7L))));
        var page = service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LIKE, null, requestedSize));
        assertThat(page.getItems()).hasSize(1);
        var nextCursor = cursorCodec.decode(page.getNextCursor(), CommunityPostCursorPayload.class);
        assertEquals(CommunityPostSort.LIKE, nextCursor.getSort());
        assertEquals(8L, nextCursor.getScore());
        assertEquals(postCreatedAt, nextCursor.getCreatedAt());
        assertEquals(postId, nextCursor.getId());
    }

    @Test
    @DisplayName("search fails when count based next cursor row has null score")
    void search_fails_when_count_based_next_cursor_row_has_null_score() throws Exception {
        var requestedSize = 1;
        var overflowPost = existingPost(member, crop, secondPostId);
        setCreatedAt(overflowPost, postCreatedAt.minusMinutes(1));
        setCreatedAt(existingPost, postCreatedAt);
        when(communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(memberId, null, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LIKE, null, requestedSize+ 1))).thenReturn(new CommunityPostQueryRepository.SearchResult(java.util.Arrays.asList(queryRow(existingPost, "https://example.test/1.jpg", 3, 8, true, null), queryRow(overflowPost, "https://example.test/2.jpg", 3, 8, true, 7L))));
        assertThrows(IllegalStateException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LIKE, null, requestedSize)));
    }

    @Test
    @DisplayName("search decodes valid incoming cursor for query repository")
    void search_decodes_valid_incoming_cursor_for_query_repository() throws Exception {
        var cursor = cursorCodec.encode(new CommunityPostCursorPayload(CommunityPostSort.LIKE, 8L, postCreatedAt, postId));
        var decodedCursor = new CommunityPostQueryRepository.Cursor(CommunityPostSort.LIKE, 8L, postCreatedAt, postId);
        when(communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(memberId, null, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LIKE, decodedCursor, 21))).thenReturn(new CommunityPostQueryRepository.SearchResult(List.of()));
        var page = service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LIKE, cursor, 20));
        assertThat(page.getItems()).isEmpty();
        assertEquals(null, page.getNextCursor());
    }

    @Test
    @DisplayName("search rejects zero size")
    void search_rejects_zero_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LATEST, null, 0)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search rejects negative size")
    void search_rejects_negative_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LATEST, null, -1)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search rejects max integer size")
    void search_rejects_max_integer_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LATEST, null, Integer.MAX_VALUE)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search rejects cursor when embedded sort differs from request sort")
    void search_rejects_cursor_when_embedded_sort_differs_from_request_sort() throws Exception {
        var cursor = cursorCodec.encode(new CommunityPostCursorPayload(CommunityPostSort.LIKE, 8L, postCreatedAt, postId));
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LATEST, cursor, 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search rejects count based sort cursor with null score")
    void search_rejects_count_based_sort_cursor_with_null_score() throws Exception {
        var cursor = cursorCodec.encode(new CommunityPostCursorPayload(CommunityPostSort.LIKE, null, postCreatedAt, postId));
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LIKE, cursor, 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("search rejects malformed cursor")
    void search_rejects_malformed_cursor() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(searchCondition(this.memberId, null, false, false, CommunityPostSort.LATEST, "not-a-valid-cursor", 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
        verifyNoInteractions(communityPostQueryRepository);
    }

    @Test
    @DisplayName("count delegates to query repository with condition and no cursor")
    void count_delegates_to_query_repository_with_condition_and_no_cursor() throws Exception {
        when(communityPostQueryRepository.count(new CommunityPostQueryRepository.SearchCondition(memberId, otherMemberId, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", false, false, CommunityPostSort.LATEST, null, 1))).thenReturn(5L);
        var total = service.count(searchCondition(this.memberId, otherMemberId, false, false, CommunityPostSort.LATEST, null, 20));
        assertEquals(5L, total);
        verifyNoInteractions(communityPostRepository);
    }
    private Member member(UUID id) {
        return new Member(id, "" + id + "@example.com", null);
    }
    private Crop crop(UUID id, String name) {
        return new Crop(id, id.hashCode(), name, CropUsePartCategory.ROOT_BARK);
    }
    private MemberCrop memberCrop(Member member, Crop crop) {
        return new MemberCrop(member, farm(member), crop);
    }
    private FarmingRecord farmingRecord(Member member, Crop crop) {
        return new FarmingRecord(recordId, member, farm(member), crop, WorkType.PLANTING, LocalDateTime.of(2026, 6, 1, 9, 0), "맑음", 20, "memo", EntryMode.MANUAL, false);
    }
    private UploadedMedia uploadedMedia(Member member, UUID id, UploadedMediaUsageType usageType, UploadedMediaStatus status) {
        var index = id.equals(mediaId2) ? "2" : "1";
        var media = new UploadedMedia(id, member, UploadedMediaType.IMAGE, usageType, "https://example.test/" + index + ".jpg", "community/" + index);
        media.setStatus(status);
        return media;
    }
    private CommunityPost existingPost(Member author, Crop crop, UUID id) {
        return new CommunityPost(id, author, crop, record, CommunityPostType.QUESTION, "황기 발아율이 낮아요", "싹이 거의 올라오지 않아요.", false);
    }
    private CommunityPostSearchCondition searchCondition(UUID memberId, UUID authorMemberId, boolean likedOnly, boolean mineOnly, CommunityPostSort sort, String cursor, int size) {
        return new CommunityPostSearchCondition(memberId, authorMemberId, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "발아", likedOnly, mineOnly, sort, cursor, size);
    }
    private CommunityPostCommand.Create createCommand(List<UUID> mediaIds, UUID farmingRecordId) {
        return new CommunityPostCommand.Create(memberId, cropId, CommunityPostType.QUESTION, "황기 발아율이 낮아요", "싹이 거의 올라오지 않아요.", farmingRecordId, mediaIds);
    }
    private CommunityPostCommand.Update updateCommand(List<UUID> mediaIds, UUID farmingRecordId) {
        return new CommunityPostCommand.Update(memberId, postId, secondCropId, CommunityPostType.GENERAL, "수정된 제목", "수정된 본문", farmingRecordId, mediaIds);
    }
    private CommunityPostQueryRepository.Row queryRow(CommunityPost post, String thumbnailUrl, long commentCount, long likeCount, boolean likedByMe, Long score) {
        return new CommunityPostQueryRepository.Row(post, thumbnailUrl, commentCount, likeCount, likedByMe, score);
    }
    private CommunityPost capturedPost() {
        var captor = ArgumentCaptor.forClass(CommunityPost.class);
        verify(communityPostRepository).save(captor.capture());
        return captor.getValue();
    }
    private List<CommunityPostMedia> capturedPostMedia() {
        ArgumentCaptor<Iterable<CommunityPostMedia>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(communityPostMediaRepository).saveAll(captor.capture());
        return java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }
    private Farm farm(Member member) {
        return new Farm(member, "약초농장", "서울시 강남구");
    }
    private void setCreatedAt(BaseTimeEntity entity, LocalDateTime createdAt) {
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "createdAt", createdAt);
    }
}
