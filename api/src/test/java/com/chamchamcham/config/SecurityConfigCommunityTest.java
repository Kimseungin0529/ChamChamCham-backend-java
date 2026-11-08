package com.chamchamcham.config;

import com.chamchamcham.api.community.controller.CommunityController;
import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.api.security.CustomAccessDeniedHandler;
import com.chamchamcham.api.security.CustomAuthenticationEntryPoint;
import com.chamchamcham.application.community.CommunityCommentResult;
import com.chamchamcham.application.community.CommunityCommentService;
import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.application.community.CommunityPostSearchCondition;
import com.chamchamcham.application.community.CommunityPostService;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@WebMvcTest(CommunityController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    MDCLoggingFilter.class,
    GlobalExceptionHandler.class
})
class SecurityConfigCommunityTest {
    @Autowired private MockMvc mockMvc;
    private final UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID authorMemberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID commentId = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @MockBean private CommunityPostService communityPostService;
    @MockBean private CommunityCommentService communityCommentService;
    @MockBean private TokenProvider tokenProvider;

    @Test
    @DisplayName("guest can read post list detail and comments")
    void guestCanReadPostListDetailAndComments() throws Exception {
        when(communityPostService.search(guestSearchCondition()))
            .thenReturn(new CommunityPostResult.Page(java.util.List.of(), null));
        when(communityPostService.getDetail(null, postId)).thenReturn(postDetail());
        when(communityCommentService.list(postId, null, 20))
            .thenReturn(new CommunityCommentResult.Page(java.util.List.of(), null));

        mockMvc.perform(get("/api/v1/community/posts"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/community/posts/{postId}", postId))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/community/posts/{postId}/comments", postId))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("guest cannot access community write routes")
    void guestCannotAccessCommunityWriteRoutes() throws Exception {
        mockMvc.perform(
            post("/api/v1/community/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/community/posts/{postId}", postId))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(
            post("/api/v1/community/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/community/comments/{commentId}", commentId))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/community/posts/{postId}/like-toggle", postId))
            .andExpect(status().isUnauthorized());
    }

    private CommunityPostSearchCondition guestSearchCondition() {
        return new CommunityPostSearchCondition(
            null,
            null,
            java.util.List.of(),
            null,
            null,
            false,
            false,
            CommunityPostSort.LATEST,
            null,
            20
        );
    }
    private CommunityPostResult.PostDetail postDetail() {
        return new CommunityPostResult.PostDetail(
            postId,
            cropId,
            "황기",
            CommunityPostType.QUESTION,
            "황기 발아율 질문",
            "싹이 거의 올라오지 않아요.",
            java.util.List.of(),
            null,
            new CommunityPostResult.AuthorSummary(
                authorMemberId,
                "황기농부",
                null
            ),
            0,
            0,
            false,
            LocalDateTime.of(2026, 6, 12, 9, 0)
        );
    }
}
