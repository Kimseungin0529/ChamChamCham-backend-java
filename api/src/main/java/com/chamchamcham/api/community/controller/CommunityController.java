package com.chamchamcham.api.community.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import com.chamchamcham.api.community.dto.CommunityRequests;
import com.chamchamcham.api.community.dto.CommunityResponses;
import com.chamchamcham.application.community.*;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import kotlin.Unit;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {
    private final CommunityPostService communityPostService;
    private final CommunityCommentService communityCommentService;

    public CommunityController(CommunityPostService communityPostService, CommunityCommentService communityCommentService) {
        this.communityPostService = communityPostService;
        this.communityCommentService = communityCommentService;
    }
    @GetMapping("/boards")
    public ResponseEntity<ApiResponse<List<CommunityResponses.BoardResponse>>> listBoards(@AuthenticationPrincipal String memberId) {
        var boards = communityPostService.listBoards(parseMemberId(memberId));
        return ResponseEntity.ok(ApiResponse.ok(boards.stream().map(CommunityResponses.BoardResponse::from).collect(Collectors.toCollection(ArrayList::new))));
    }
    @GetMapping("/members/{memberId}/post-crops")
    public ResponseEntity<ApiResponse<List<CommunityResponses.BoardResponse>>> listPostCrops(@AuthenticationPrincipal String authenticatedMemberId,
        @PathVariable("memberId") UUID memberId) {
        parseMemberId(authenticatedMemberId);
        var crops = communityPostService.listPostCrops(memberId);
        return ResponseEntity.ok(ApiResponse.ok(crops.stream().map(CommunityResponses.BoardResponse::from).collect(Collectors.toCollection(ArrayList::new))));
    }
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityResponses.PostPageResponse>> listPosts(
        @AuthenticationPrincipal String memberId,
        @RequestParam(name = "authorMemberId", required = false) UUID authorMemberId,
        @RequestParam(name = "cropId", required = false) List<UUID> cropIds,
        @RequestParam(name = "postType", required = false) CommunityPostType postType,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "likedOnly", defaultValue = "false") boolean likedOnly,
        @RequestParam(name = "mineOnly", defaultValue = "false") boolean mineOnly,
        @RequestParam(name = "sort", defaultValue = "LATEST") CommunityPostSort sort,
        @RequestParam(name = "cursor", required = false) String cursor,
        @RequestParam(name = "size", defaultValue = "20") int size) throws JsonProcessingException {
        var page = communityPostService.search(new CommunityPostSearchCondition(parseOptionalMemberId(memberId), authorMemberId,
            cropIds == null ? Collections.emptyList() : cropIds, postType, keyword, likedOnly, mineOnly, sort, cursor, size));
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostPageResponse.from(page)));
    }
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityResponses.PostIdResponse>> createPost(@AuthenticationPrincipal String memberId,
        @Valid @RequestBody CommunityRequests.SavePostRequest request) {
        var result = communityPostService.create(new CommunityPostCommand.Create(parseMemberId(memberId),
            java.util.Objects.requireNonNull(request.cropId()),
            java.util.Objects.requireNonNull(request.postType()), request.title(), request.body(), request.farmingRecordId(), request.mediaIds()));
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostIdResponse.from(result)));
    }
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityResponses.PostDetailResponse>> getPost(@AuthenticationPrincipal String memberId,
        @PathVariable("postId") UUID postId) {
        var detail = communityPostService.getDetail(parseOptionalMemberId(memberId), postId);
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostDetailResponse.from(detail)));
    }
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<CommunityResponses.PostIdResponse>> updatePost(@AuthenticationPrincipal String memberId,
        @PathVariable("postId") UUID postId,
        @Valid @RequestBody CommunityRequests.SavePostRequest request) {
        var result = communityPostService.update(new CommunityPostCommand.Update(parseMemberId(memberId),
            postId,
            java.util.Objects.requireNonNull(request.cropId()),
            java.util.Objects.requireNonNull(request.postType()), request.title(), request.body(), request.farmingRecordId(), request.mediaIds()));
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.PostIdResponse.from(result)));
    }
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Unit>> deletePost(@AuthenticationPrincipal String memberId, @PathVariable("postId") UUID postId) {
        communityPostService.delete(new CommunityPostCommand.Delete(parseMemberId(memberId), postId));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommunityResponses.CommentPageResponse>> listComments(@PathVariable("postId") UUID postId,
        @RequestParam(name = "cursor",
        required = false) String cursor,
        @RequestParam(name = "size",
        defaultValue = "20") int size) throws JsonProcessingException {
        var page = communityCommentService.list(postId, cursor, size);
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.CommentPageResponse.from(page)));
    }
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommunityResponses.CommentIdResponse>> createComment(@AuthenticationPrincipal String memberId,
        @PathVariable("postId") UUID postId, @Valid @RequestBody CommunityRequests.CreateCommentRequest request) {
        var result = communityCommentService.create(new CommunityCommentCommand.Create(parseMemberId(memberId),
            postId,
            request.parentCommentId(),
            request.body(),
            request.mediaId()));
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.CommentIdResponse.from(result)));
    }
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Unit>> deleteComment(@AuthenticationPrincipal String memberId, @PathVariable("commentId") UUID commentId) {
        communityCommentService.delete(new CommunityCommentCommand.Delete(parseMemberId(memberId), commentId));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }
    @PostMapping("/posts/{postId}/like-toggle")
    public ResponseEntity<ApiResponse<CommunityResponses.LikeToggleResponse>> toggleLike(@AuthenticationPrincipal String memberId,
        @PathVariable("postId") UUID postId) {
        var result = communityPostService.toggleLike(new CommunityPostCommand.ToggleLike(parseMemberId(memberId), postId));
        return ResponseEntity.ok(ApiResponse.ok(CommunityResponses.LikeToggleResponse.from(result)));
    }

    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }

    private UUID parseOptionalMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return null;
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { return null; }
    }
}
