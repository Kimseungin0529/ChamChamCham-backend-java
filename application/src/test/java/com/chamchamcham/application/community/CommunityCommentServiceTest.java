package com.chamchamcham.application.community;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.community.CommunityComment;
import com.chamchamcham.domain.community.CommunityCommentRepository;
import com.chamchamcham.domain.community.CommunityPost;
import com.chamchamcham.domain.community.CommunityPostRepository;
import com.chamchamcham.domain.community.CommunityPostType;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.data.domain.PageRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class CommunityCommentServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID rootCommentId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID replyCommentId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000601");
    @Mock private MemberRepository memberRepository;
    @Mock private CommunityPostRepository communityPostRepository;
    @Mock private CommunityCommentRepository communityCommentRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    private CommunityCommentService service;
    private Member member;
    private Member otherMember;
    private CommunityPost post;
    private CommunityComment rootComment;
    private CommunityComment replyComment;

    @BeforeEach
    void setUp() throws Exception {
        member = new Member(memberId, "farmer@example.com", null);
        otherMember = new Member(otherMemberId, "other@example.com", null);
        post = new CommunityPost(postId, member, new Crop(UUID.fromString("00000000-0000-0000-0000-000000000301"), 422, "황기",
            CropUsePartCategory.ROOT_BARK), CommunityPostType.QUESTION, "황기 발아율이 낮아요", "싹이 거의 올라오지 않아요.");
        rootComment = comment(rootCommentId, null, member, "저도 궁금해요", null, false);
        replyComment = comment(replyCommentId, rootComment, otherMember, "스크래치 작업을 해보세요", null, false);
        service = new CommunityCommentService(memberRepository, communityPostRepository, communityCommentRepository, uploadedMediaRepository, cursorCodec);
    }

    @Test
    @DisplayName("create root comment stores comment on active post")
    void create_root_comment_stores_comment_on_active_post() throws Exception {
        stubCreate();
        when(communityCommentRepository.save(any(CommunityComment.class))).thenAnswer(invocation -> {
            var comment = invocation.<CommunityComment>getArgument(0);
            return comment(rootCommentId, comment.getParentComment(), comment.getAuthor(), comment.getBody(), null, false);
        });
        var result = service.create(createCommand(null, "저도 궁금해요", null));
        assertEquals(rootCommentId, result.getId());
        var savedComment = capturedComment();
        assertEquals(postId, savedComment.getPost().getId());
        assertNull(savedComment.getParentComment());
        assertEquals(memberId, savedComment.getAuthor().getId());
        assertEquals("저도 궁금해요", savedComment.getBody());
    }

    @Test
    @DisplayName("create reply stores one level reply under root comment")
    void create_reply_stores_one_level_reply_under_root_comment() throws Exception {
        stubCreate();
        when(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(rootComment));
        when(communityCommentRepository.save(any(CommunityComment.class))).thenAnswer(invocation -> {
            var comment = invocation.<CommunityComment>getArgument(0);
            return comment(replyCommentId, comment.getParentComment(), comment.getAuthor(), comment.getBody(), null, false);
        });
        var result = service.create(createCommand(rootCommentId, "스크래치 작업을 해보세요", null));
        assertEquals(replyCommentId, result.getId());
        var savedComment = capturedComment();
        assertEquals(rootCommentId, savedComment.getParentComment().getId());
        assertEquals("스크래치 작업을 해보세요", savedComment.getBody());
    }

    @Test
    @DisplayName("create reply rejects reply parent that is already a reply")
    void create_reply_rejects_reply_parent_that_is_already_a_reply() throws Exception {
        stubCreate();
        when(communityCommentRepository.findById(replyCommentId)).thenReturn(Optional.of(replyComment));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(replyCommentId, "저도 궁금해요", null)));
        assertEquals(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("create reply rejects deleted parent comment")
    void create_reply_rejects_deleted_parent_comment() throws Exception {
        var deletedParent = comment(rootCommentId, null, member, "삭제 전", null, true);
        stubCreate();
        when(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(deletedParent));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(rootCommentId, "저도 궁금해요", null)));
        assertEquals(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("delete soft deletes author comment")
    void delete_soft_deletes_author_comment() throws Exception {
        when(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(rootComment));
        service.delete(new CommunityCommentCommand.Delete(memberId, rootCommentId));
        assertTrue(rootComment.isDeleted());
    }

    @Test
    @DisplayName("list returns root comment page with replies and next cursor")
    void list_returns_root_comment_page_with_replies_and_next_cursor() throws Exception {
        var newest = comment(rootCommentId, null, member, "newest", null, false);
        var older = comment(UUID.fromString("00000000-0000-0000-0000-000000000203"), null, member, "older", null, false);
        var overflow = comment(UUID.fromString("00000000-0000-0000-0000-000000000204"), null, member, "overflow", null, false);
        var reply = comment(replyCommentId, newest, otherMember, "reply", null, false);
        setCreatedAt(newest, LocalDateTime.of(2026, 6, 12, 11, 0));
        setCreatedAt(older, LocalDateTime.of(2026, 6, 12, 10, 0));
        setCreatedAt(overflow, LocalDateTime.of(2026, 6, 12, 9, 0));
        setCreatedAt(reply, LocalDateTime.of(2026, 6, 12, 11, 1));
        when(communityCommentRepository.findRootFirstPage(postId, PageRequest.of(0, 3))).thenReturn(java.util.Arrays.asList(newest, older, overflow));
        when(communityCommentRepository.findRepliesByParentIds(java.util.Arrays.asList(rootCommentId,
            Objects.requireNonNull(older.getId())))).thenReturn(java.util.Arrays.asList(reply));
        var page = service.list(postId, null, 2);
        assertEquals(java.util.Arrays.asList("newest", "older"), page.getItems().stream().map(it -> it.getBody()).toList());
        assertEquals(java.util.Arrays.asList("reply"), page.getItems().getFirst().getReplies().stream().map(it -> it.getBody()).toList());
        assertTrue(Objects.equals(page.getNextCursor().isBlank() == false, true));
    }

    @Test
    @DisplayName("list passes decoded cursor to repository")
    void list_passes_decoded_cursor_to_repository() throws Exception {
        var payload = new CommunityCommentCursorPayload(LocalDateTime.of(2026, 6, 12, 10, 0), rootCommentId);
        var cursor = cursorCodec.encode(payload);
        when(communityCommentRepository.findRootPageAfter(postId, payload.getCreatedAt(), payload.getId(), PageRequest.of(0, 21))).thenReturn(List.of());
        var page = service.list(postId, cursor, 20);
        assertTrue(page.getItems().isEmpty());
        assertNull(page.getNextCursor());
    }

    @Test
    @DisplayName("list rejects invalid page size")
    void list_rejects_invalid_page_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.list(postId, null, 0));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("list rejects malformed cursor")
    void list_rejects_malformed_cursor() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.list(postId, "not-a-valid-cursor", 20));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
    }

    @Test
    @DisplayName("deleted comment hides image url")
    void deleted_comment_hides_image_url() throws Exception {
        var media = uploadedMedia(mediaId, member, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        var deleted = comment(rootCommentId, null, member, "before delete", media, true);
        setCreatedAt(deleted, LocalDateTime.of(2026, 6, 12, 11, 0));
        when(communityCommentRepository.findRootFirstPage(postId, PageRequest.of(0, 21))).thenReturn(java.util.Arrays.asList(deleted));
        when(communityCommentRepository.findRepliesByParentIds(java.util.Arrays.asList(rootCommentId))).thenReturn(List.of());
        var page = service.list(postId, null, 20);
        assertEquals(1, page.getItems().size());
        assertEquals("삭제된 댓글입니다.", page.getItems().get(0).getBody());
        assertNull(page.getItems().get(0).getImageUrl());
    }

    @Test
    @DisplayName("create comment attaches optional media")
    void create_comment_attaches_optional_media() throws Exception {
        var media = uploadedMedia(mediaId, member, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.TEMP);
        stubCreate();
        when(uploadedMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(communityCommentRepository.save(any(CommunityComment.class))).thenAnswer(invocation -> {
            var comment = invocation.<CommunityComment>getArgument(0);
            return comment(rootCommentId, comment.getParentComment(), comment.getAuthor(), comment.getBody(), comment.getMedia(), false);
        });
        service.create(createCommand(null, "저도 궁금해요", mediaId));
        var savedComment = capturedComment();
        assertEquals(mediaId, savedComment.getMedia().getId());
        assertEquals(UploadedMediaStatus.ATTACHED, media.getStatus());
    }

    @Test
    @DisplayName("create comment rejects missing media")
    void create_comment_rejects_missing_media() throws Exception {
        stubCreate();
        when(uploadedMediaRepository.findById(mediaId)).thenReturn(Optional.empty());
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(null, "저도 궁금해요", mediaId)));
        assertEquals(ErrorCode.MEDIA_NOT_FOUND, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("create comment rejects media owned by another member")
    void create_comment_rejects_media_owned_by_another_member() throws Exception {
        stubCreate();
        when(uploadedMediaRepository.findById(mediaId)).thenReturn(Optional.of(uploadedMedia(mediaId, otherMember, UploadedMediaUsageType.COMMUNITY_POST,
            UploadedMediaStatus.TEMP)));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(null, "저도 궁금해요", mediaId)));
        assertEquals(ErrorCode.MEDIA_NOT_OWNED, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("create comment rejects media with wrong usage type")
    void create_comment_rejects_media_with_wrong_usage_type() throws Exception {
        var media = uploadedMedia(mediaId, member, UploadedMediaUsageType.PROFILE, UploadedMediaStatus.TEMP);
        stubCreate();
        when(uploadedMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(null, "저도 궁금해요", mediaId)));
        assertEquals(ErrorCode.MEDIA_USAGE_MISMATCH, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }

    @Test
    @DisplayName("create comment rejects already attached media")
    void create_comment_rejects_already_attached_media() throws Exception {
        var media = uploadedMedia(mediaId, member, UploadedMediaUsageType.COMMUNITY_POST, UploadedMediaStatus.ATTACHED);
        stubCreate();
        when(uploadedMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(null, "저도 궁금해요", mediaId)));
        assertEquals(ErrorCode.MEDIA_NOT_ATTACHABLE, exception.getErrorCode());
        verify(communityCommentRepository, never()).save(any(CommunityComment.class));
    }
    @Test
    void visibleRootsAreSnapshotBeforeReplyRepositoryCall() throws Exception {
        setCreatedAt(rootComment, LocalDateTime.of(2026, 6, 12, 11, 0));
        var roots = new ArrayList<>(List.of(rootComment));
        when(communityCommentRepository.findRootFirstPage(postId, PageRequest.of(0, 21))).thenReturn(roots);
        when(communityCommentRepository.findRepliesByParentIds(List.of(rootCommentId))).thenAnswer(invocation -> {
            roots.clear();
            return List.of();
        });
        var page = service.list(postId, null, 20);
        assertEquals(List.of("저도 궁금해요"), page.getItems().stream().map(CommunityCommentResult.Comment::getBody).toList());
    }

    private void stubCreate() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(post);
    }
    private CommunityCommentCommand.Create createCommand(UUID parentCommentId, String body, UUID mediaId) {
        return new CommunityCommentCommand.Create(memberId, postId, parentCommentId, body, mediaId);
    }
    private CommunityComment comment(UUID id, CommunityComment parent, Member author, String body, UploadedMedia media, boolean isDeleted) {
        return new CommunityComment(id, post, parent, author, body, media, isDeleted);
    }
    private UploadedMedia uploadedMedia(UUID id, Member owner, UploadedMediaUsageType usageType, UploadedMediaStatus status) {
        var media = new UploadedMedia(id, owner, UploadedMediaType.IMAGE, usageType, "https://example.test/comment.jpg", "community/comment");
        media.setStatus(status);
        return media;
    }
    private CommunityComment capturedComment() {
        var captor = ArgumentCaptor.forClass(CommunityComment.class);
        verify(communityCommentRepository).save(captor.capture());
        return captor.getValue();
    }
    private void setCreatedAt(BaseTimeEntity entity, LocalDateTime createdAt) {
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "createdAt", createdAt);
    }
}
