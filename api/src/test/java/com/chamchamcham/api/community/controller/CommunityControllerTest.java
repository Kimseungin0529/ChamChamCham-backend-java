package com.chamchamcham.api.community.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.community.CommunityCommentCommand;
import com.chamchamcham.application.community.CommunityCommentResult;
import com.chamchamcham.application.community.CommunityCommentService;
import com.chamchamcham.application.community.CommunityPostCommand;
import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.application.community.CommunityPostSearchCondition;
import com.chamchamcham.application.community.CommunityPostService;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(CommunityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CommunityControllerTest {
@Autowired private MockMvc mockMvc;
private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
private final UUID authorMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
private final UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000101");
private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
private final UUID secondCropId = UUID.fromString("00000000-0000-0000-0000-000000000202");
private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000301");
private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000401");
private final UUID commentId = UUID.fromString("00000000-0000-0000-0000-000000000501");
private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 12, 9, 0);
@MockBean private CommunityPostService communityPostService;
@MockBean private CommunityCommentService communityCommentService;
@MockBean private TokenProvider tokenProvider;
@Test
@DisplayName("create post maps request to command and returns post id")
void create_post_maps_request_to_command_and_returns_post_id() throws Exception {
when(communityPostService.create(createPostCommand())).thenReturn(new CommunityPostResult.PostId(postId));
mockMvc.perform(post("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(savePostJson("[\"%s\"]".formatted(mediaId))))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.id", equalTo(postId.toString())));
}
@Test
@DisplayName("create post rejects more than five media ids")
void create_post_rejects_more_than_five_media_ids() throws Exception {
var mediaIds = java.util.stream.IntStream.range(0, 6).mapToObj(it -> "\"%s\"".formatted(UUID.randomUUID())).collect(java.util.stream.Collectors.joining(","));
mockMvc.perform(post("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(savePostJson("[%s]".formatted(mediaIds))))
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
.andExpect(jsonPath("$.error.detail.field", equalTo("mediaIds")));
}
@Test
@DisplayName("list post crops returns crops for the path member id not the authenticated caller")
void list_post_crops_returns_crops_for_the_path_member_id_not_the_authenticated_caller() throws Exception {
when(communityPostService.listPostCrops(authorMemberId)).thenReturn(java.util.Arrays.asList(new CommunityPostResult.Board(cropId, "황기"), new CommunityPostResult.Board(secondCropId, "인삼")));
mockMvc.perform(get("/api/v1/community/members/%s/post-crops".formatted(authorMemberId))
                .with(authenticatedMember(memberId.toString())))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data[0].cropId", equalTo(cropId.toString())))
.andExpect(jsonPath("$.data[1].cropId", equalTo(secondCropId.toString())));
}
@Test
@DisplayName("list posts returns cursor page")
void list_posts_returns_cursor_page() throws Exception {
when(communityPostService.search(anySearchCondition(this.memberId))).thenReturn(postPageResult());
mockMvc.perform(get("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("size", "20"))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.items[0].id", equalTo(postId.toString())))
.andExpect(jsonPath("$.data.items[0].likedByMe", equalTo(true)))
.andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-1")));
}
@Test
@DisplayName("guest list maps null viewer")
void guest_list_maps_null_viewer() throws Exception {
when(communityPostService.search(anySearchCondition(null)))
.thenReturn(new CommunityPostResult.Page(java.util.Collections.emptyList(), null));
mockMvc.perform(get("/api/v1/community/posts"))
.andExpect(status().isOk());
}
@Test
@DisplayName("list posts maps sort and cursor parameters")
void list_posts_maps_sort_and_cursor_parameters() throws Exception {
when(communityPostService.search(new CommunityPostSearchCondition(memberId, null, java.util.Arrays.asList(cropId), CommunityPostType.QUESTION, "황기", true, false, CommunityPostSort.POPULAR, "cursor-1", 10))).thenReturn(postPageResult());
mockMvc.perform(get("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("cropId", cropId.toString())
                .param("postType", "QUESTION")
                .param("keyword", "황기")
                .param("likedOnly", "true")
                .param("sort", "POPULAR")
                .param("cursor", "cursor-1")
                .param("size", "10"))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-1")));
}
@Test
@DisplayName("list posts maps multiple crop ids")
void list_posts_maps_multiple_crop_ids() throws Exception {
var cropIds = java.util.Arrays.asList(cropId, secondCropId);
when(communityPostService.search(new CommunityPostSearchCondition(memberId, null, cropIds, null, null, false, false, CommunityPostSort.LATEST, null, 20))).thenReturn(postPageResult());
mockMvc.perform(get("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("cropId", cropId.toString(), secondCropId.toString()))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.items[0].id", equalTo(postId.toString())));
}
@Test
@DisplayName("list posts maps author member id separately from authenticated member id")
void list_posts_maps_author_member_id_separately_from_authenticated_member_id() throws Exception {
when(communityPostService.search(new CommunityPostSearchCondition(memberId, authorMemberId, java.util.Collections.emptyList(), null, null, false, false, CommunityPostSort.LATEST, null, 20))).thenReturn(postPageResult());
mockMvc.perform(get("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("authorMemberId", authorMemberId.toString()))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.items[0].id", equalTo(postId.toString())));
}
@Test
@DisplayName("list posts returns invalid input for invalid sort parameter")
void list_posts_returns_invalid_input_for_invalid_sort_parameter() throws Exception {
mockMvc.perform(get("/api/v1/community/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("sort", "BAD_VALUE"))
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
.andExpect(jsonPath("$.error.detail.field", equalTo("sort")));
}
@Test
@DisplayName("get post detail returns detail")
void get_post_detail_returns_detail() throws Exception {
when(communityPostService.getDetail(memberId, postId)).thenReturn(postDetailResult(true));
mockMvc.perform(get("/api/v1/community/posts/{postId}", postId)
                .with(authenticatedMember(memberId.toString())))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.title", equalTo("황기 발아율 질문")));
}
@Test
@DisplayName("guest detail maps null viewer")
void guest_detail_maps_null_viewer() throws Exception {
when(communityPostService.getDetail(null, postId)).thenReturn(postDetailResult(false));
mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.likedByMe", equalTo(false)));
}
@Test
@DisplayName("update post maps request to command")
void update_post_maps_request_to_command() throws Exception {
when(communityPostService.update(updatePostCommand())).thenReturn(new CommunityPostResult.PostId(postId));
mockMvc.perform(patch("/api/v1/community/posts/{postId}", postId)
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(savePostJson("[\"%s\"]".formatted(mediaId))))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.id", equalTo(postId.toString())));
}
@Test
@DisplayName("delete post delegates to service")
void delete_post_delegates_to_service() throws Exception {
mockMvc.perform(delete("/api/v1/community/posts/{postId}", postId)
                .with(authenticatedMember(memberId.toString())))
.andExpect(status().isOk())
.andExpect(jsonPath("$.success", equalTo(true)));
}
@Test
@DisplayName("create comment maps request to command")
void create_comment_maps_request_to_command() throws Exception {
when(communityCommentService.create(createCommentCommand()))
.thenReturn(new CommunityCommentResult.CommentId(commentId));
mockMvc.perform(post("/api/v1/community/posts/{postId}/comments", postId)
                .with(authenticatedMember(memberId.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
{"body":"저도 궁금해요","mediaId":"%s"}""".formatted(mediaId)))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.id", equalTo(commentId.toString())));
}
@Test
@DisplayName("list comments returns cursor page with deleted comment body from service result")
void list_comments_returns_cursor_page_with_deleted_comment_body_from_service_result() throws Exception {
when(communityCommentService.list(postId, null, 20)).thenReturn(commentPageResult("comment-cursor-1"));
mockMvc.perform(get("/api/v1/community/posts/{postId}/comments", postId)
                .with(authenticatedMember(memberId.toString())))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.items[0].body", equalTo("삭제된 댓글입니다.")))
.andExpect(jsonPath("$.data.items[0].imageUrl", nullValue()))
.andExpect(jsonPath("$.data.nextCursor", equalTo("comment-cursor-1")));
}
@Test
@DisplayName("list comments maps cursor and size parameters")
void list_comments_maps_cursor_and_size_parameters() throws Exception {
when(communityCommentService.list(postId, "comment-cursor-1", 10)).thenReturn(commentPageResult(null));
mockMvc.perform(get("/api/v1/community/posts/{postId}/comments", postId)
                .with(authenticatedMember(memberId.toString()))
                .param("cursor", "comment-cursor-1")
                .param("size", "10"))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.nextCursor", nullValue()));
}
@Test
@DisplayName("toggle like returns liked state and count")
void toggle_like_returns_liked_state_and_count() throws Exception {
when(communityPostService.toggleLike(new CommunityPostCommand.ToggleLike(memberId, postId)))
.thenReturn(new CommunityPostResult.LikeToggle(true, 9));
mockMvc.perform(post("/api/v1/community/posts/{postId}/like-toggle", postId)
                .with(authenticatedMember(memberId.toString())))
.andExpect(status().isOk())
.andExpect(jsonPath("$.data.liked", equalTo(true)))
.andExpect(jsonPath("$.data.likeCount", equalTo(9)));
}

@Test
void requestDefaultsAndExplicitNullKeepTheirJsonBoundary() throws Exception {
    var command = new CommunityPostCommand.Create(memberId, cropId, CommunityPostType.QUESTION,
        "제목", "본문", null, Collections.emptyList());
    when(communityPostService.create(command)).thenReturn(new CommunityPostResult.PostId(postId));
    var json = "{\"cropId\":\"%s\",\"postType\":\"QUESTION\",\"title\":\"제목\",\"body\":\"본문\"}".formatted(cropId);
    mockMvc.perform(post("/api/v1/community/posts").with(authenticatedMember(memberId.toString()))
        .contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.id", equalTo(postId.toString())));
    for (var field : List.of("mediaIds", "title")) {
        var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).putNull(field);
        mockMvc.perform(post("/api/v1/community/posts").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(node.toString()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_002")));
    }
}


private RequestPostProcessor authenticatedMember(String memberId) {
return (RequestPostProcessor) (request -> {
SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(memberId, null, java.util.Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"))));
return request;
});
}
private String savePostJson(String mediaIdsJson) {
return """
            {
              "cropId":"%s",
              "postType":"QUESTION",
              "title":"황기 발아율 질문",
              "body":"싹이 거의 올라오지 않아요.",
              "farmingRecordId":"%s",
              "mediaIds":%s
            }
        """.formatted(cropId, recordId, mediaIdsJson);
}
private CommunityPostCommand.Create createPostCommand() {
return new CommunityPostCommand.Create(memberId, cropId, CommunityPostType.QUESTION, "황기 발아율 질문", "싹이 거의 올라오지 않아요.", recordId, java.util.Arrays.asList(mediaId));
}
private CommunityPostCommand.Update updatePostCommand() {
return new CommunityPostCommand.Update(memberId, postId, cropId, CommunityPostType.QUESTION, "황기 발아율 질문", "싹이 거의 올라오지 않아요.", recordId, java.util.Arrays.asList(mediaId));
}
private CommunityCommentCommand.Create createCommentCommand() {
return new CommunityCommentCommand.Create(memberId, postId, null, "저도 궁금해요", mediaId);
}
private CommunityPostResult.Page postPageResult() {
return new CommunityPostResult.Page(java.util.Arrays.asList(postSummaryResult()), "cursor-1");
}
private CommunityPostResult.PostDetail postDetailResult(boolean likedByMe) {
return new CommunityPostResult.PostDetail(postId, cropId, "황기", CommunityPostType.QUESTION, "황기 발아율 질문", "싹이 거의 올라오지 않아요.", java.util.Arrays.asList("https://example.test/1.jpg"), recordId, authorResult(), 3, 8, likedByMe, createdAt);
}
private CommunityPostResult.PostSummary postSummaryResult() {
return new CommunityPostResult.PostSummary(postId, cropId, "황기", CommunityPostType.QUESTION, "황기 발아율 질문", "싹이 거의 올라오지 않아요.", "https://example.test/1.jpg", authorResult(), 3, 8, true, createdAt);
}
private CommunityCommentResult.Page commentPageResult(String nextCursor) {
return new CommunityCommentResult.Page(java.util.Arrays.asList(deletedCommentResult()), nextCursor);
}
private CommunityCommentResult.Comment deletedCommentResult() {
return new CommunityCommentResult.Comment(commentId, null, authorResult(), "삭제된 댓글입니다.", null, true, createdAt, java.util.Collections.emptyList());
}
private CommunityPostResult.AuthorSummary authorResult() {
return new CommunityPostResult.AuthorSummary(memberId, "황기농부", "https://example.test/profile.jpg");
}
private CommunityPostSearchCondition anySearchCondition(UUID memberId) {
return new CommunityPostSearchCondition(memberId, null, java.util.Collections.emptyList(), null, null, false, false, CommunityPostSort.LATEST, null, 20);
}
}
