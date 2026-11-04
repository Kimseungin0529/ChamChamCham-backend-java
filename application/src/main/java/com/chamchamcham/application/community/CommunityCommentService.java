package com.chamchamcham.application.community;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.community.*;
import com.chamchamcham.domain.media.*;
import com.chamchamcham.domain.member.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.Nullable;
import java.util.*;

@Service
@Transactional
public class CommunityCommentService {
    private final MemberRepository memberRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final OpaqueCursorCodec cursorCodec;
    private static final String DELETED_COMMENT_BODY = "삭제된 댓글입니다.";

    public CommunityCommentService(MemberRepository memberRepository, CommunityPostRepository communityPostRepository,
                                   CommunityCommentRepository communityCommentRepository, UploadedMediaRepository uploadedMediaRepository,
                                   OpaqueCursorCodec cursorCodec) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.communityPostRepository = Objects.requireNonNull(communityPostRepository, "communityPostRepository");
        this.communityCommentRepository = Objects.requireNonNull(communityCommentRepository, "communityCommentRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    public CommunityCommentResult.CommentId create(CommunityCommentCommand.Create command) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findById(command.getMemberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var post = communityPostRepository.findByIdAndIsDeletedFalse(command.getPostId());
        if (post == null) throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        var parent = command.getParentCommentId() == null ? null : findValidParent(command.getParentCommentId());
        var media = command.getMediaId() == null ? null : validateMedia(command.getMemberId(), command.getMediaId());
        var comment = communityCommentRepository.save(new CommunityComment(null, post, parent, member, command.getBody(), media, false));
        if (media != null) media.markAttached();
        return new CommunityCommentResult.CommentId(required(comment.getId(), "Persisted comment id is required"));
    }

    public void delete(CommunityCommentCommand.Delete command) {
        Objects.requireNonNull(command, "command");
        var comment = communityCommentRepository.findById(command.getCommentId()).orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        if (!Objects.equals(comment.getAuthor().getId(), command.getMemberId())) throw new BusinessException(ErrorCode.COMMUNITY_FORBIDDEN);
        comment.softDelete();
    }

    @Transactional(readOnly = true)
    public CommunityCommentResult.Page list(UUID postId, @Nullable String cursor, int size) throws JsonProcessingException {
        Objects.requireNonNull(postId, "postId");
        if (size <= 0 || size == Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_INPUT);
        var decoded = decodeCursor(cursor);
        var pageable = PageRequest.of(0, size + 1);
        var roots = decoded == null ? communityCommentRepository.findRootFirstPage(postId, pageable)
            : communityCommentRepository.findRootPageAfter(postId, decoded.getCreatedAt(), decoded.getId(), pageable);
        var visible = new ArrayList<>(roots.subList(0, Math.min(size, roots.size())));
        var ids = new ArrayList<UUID>();
        for (var root : visible) ids.add(required(root.getId(), "Persisted comment id is required"));
        var repliesByParentId = new LinkedHashMap<UUID, List<CommunityComment>>();
        if (!ids.isEmpty()) {
            for (var reply : communityCommentRepository.findRepliesByParentIds(ids)) {
                var parentId = required(reply.getParentComment() == null ? null : reply.getParentComment().getId(), "Persisted parent comment id is required");
                repliesByParentId.computeIfAbsent(parentId, key -> new ArrayList<>()).add(reply);
            }
        }
        var nextCursor = roots.size() > size && !visible.isEmpty() ? encodeCursor(visible.getLast()) : null;
        var items = new ArrayList<CommunityCommentResult.Comment>();
        for (var root : visible) {
            var replies = new ArrayList<CommunityCommentResult.Comment>();
            for (var reply : repliesByParentId.getOrDefault(required(root.getId(), "Persisted comment id is required"),
                List.of())) replies.add(toResult(reply, List.of()));
            items.add(toResult(root, replies));
        }
        return new CommunityCommentResult.Page(items, nextCursor);
    }

    private CommunityComment findValidParent(UUID id) {
        var parent = communityCommentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        if (parent.isDeleted() || parent.getParentComment() != null) throw new BusinessException(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT);
        return parent;
    }

    private CommunityCommentCursorPayload decodeCursor(String cursor) {
        if (cursor == null || cursor.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return null;
        return cursorCodec.decode(cursor, CommunityCommentCursorPayload.class);
    }

    private String encodeCursor(CommunityComment comment) throws JsonProcessingException {
        return cursorCodec.encode(new CommunityCommentCursorPayload(comment.getCreatedAt(), required(comment.getId(), "Persisted comment id is required")));
    }

    private UploadedMedia validateMedia(UUID memberId, UUID mediaId) {
        var media = uploadedMediaRepository.findById(mediaId).orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        if (!Objects.equals(media.getOwner().getId(), memberId)) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
        if (media.getUsageType() != UploadedMediaUsageType.COMMUNITY_POST) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
        if (!media.isAttachable()) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
        return media;
    }

    private CommunityCommentResult.Comment toResult(CommunityComment comment, List<CommunityCommentResult.Comment> replies) {
        return new CommunityCommentResult.Comment(required(comment.getId(), "Persisted comment id is required"),
            comment.getParentComment() == null ? null : comment.getParentComment().getId(), authorOf(comment.getAuthor()),
            comment.isDeleted() ? DELETED_COMMENT_BODY : comment.getBody(),
            comment.isDeleted() || comment.getMedia() == null ? null : comment.getMedia().getFileUrl(),
            comment.isDeleted(), comment.getCreatedAt(), replies);
    }

    private CommunityPostResult.AuthorSummary authorOf(Member member) {
        return new CommunityPostResult.AuthorSummary(required(member.getId(), "Persisted member id is required"), member.getNickname(),
            member.getProfileMedia() == null ? null : member.getProfileMedia().getFileUrl());
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
