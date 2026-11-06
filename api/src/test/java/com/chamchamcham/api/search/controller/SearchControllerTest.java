package com.chamchamcham.api.search.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.application.farming.FarmingRecordResult;
import com.chamchamcham.application.search.SearchQuery;
import com.chamchamcham.application.search.SearchResult;
import com.chamchamcham.application.search.SearchService;
import com.chamchamcham.application.search.SearchSuggestionResult;
import com.chamchamcham.application.search.SearchSuggestionService;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.community.CommunityPostType;
import com.chamchamcham.domain.farming.WorkType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SearchControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID policyId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 9, 0);

    @MockBean private SearchService searchService;
    @MockBean private SearchSuggestionService searchSuggestionService;
    @MockBean private TokenProvider tokenProvider;


    @Test
    @DisplayName("search all returns record policy and post sections")
    void searchAllReturnsRecordPolicyAndPostSections() throws Exception {
        when(searchService.searchAll(memberId, "황기")).thenReturn(
            new SearchResult.All(
                new SearchResult.RecordPage(List.of(recordSummary()), null, 12),
                new SearchResult.PolicyPage(List.of(policyItem()), null, 7),
                new SearchResult.PostPage(List.of(postSummary()), null, 5))
        );

        mockMvc.perform(
            get("/api/v1/search")
                .with(authenticatedMember(memberId.toString()))
                .param("keyword", "황기")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records.items[0].id", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.records.items[0].cropName", equalTo("황기")))
            .andExpect(jsonPath("$.data.records.items[0].workType", equalTo("HARVEST")))
            .andExpect(jsonPath("$.data.records.totalCount", equalTo(12)))
            .andExpect(jsonPath("$.data.policies.items[0].id", equalTo(policyId.toString())))
            .andExpect(jsonPath("$.data.policies.items[0].agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.policies.totalCount", equalTo(7)))
            .andExpect(jsonPath("$.data.posts.items[0].id", equalTo(postId.toString())))
            .andExpect(jsonPath("$.data.posts.items[0].likeCount", equalTo(5)))
            .andExpect(jsonPath("$.data.posts.totalCount", equalTo(5)));
    }

    @Test
    @DisplayName("search records returns cursor page with default size 20")
    void searchRecordsReturnsCursorPageWithDefaultSize20() throws Exception {
        when(
            searchService.searchRecords(
                new SearchQuery(memberId, "황기", null, 20)
            )
        ).thenReturn(
            new SearchResult.RecordPage(List.of(recordSummary()), "cursor-2", 25)
        );

        mockMvc.perform(
            get("/api/v1/search/records")
                .with(authenticatedMember(memberId.toString()))
                .param("keyword", "황기")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].id", equalTo(recordId.toString())))
            .andExpect(jsonPath("$.data.items[0].memoPreview", equalTo("수확 완료")))
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-2")))
            .andExpect(jsonPath("$.data.totalCount", equalTo(25)));
    }

    @Test
    @DisplayName("search policies returns policy fields and forwards cursor and size")
    void searchPoliciesReturnsPolicyFieldsAndForwardsCursorAndSize() throws Exception {
        when(
            searchService.searchPolicies(
                new SearchQuery(memberId, "청년", "cursor-1", 10)
            )
        ).thenReturn(
            new SearchResult.PolicyPage(List.of(policyItem()), null, 1)
        );

        mockMvc.perform(
            get("/api/v1/search/policies")
                .with(authenticatedMember(memberId.toString()))
                .param("keyword", "청년")
                .param("cursor", "cursor-1")
                .param("size", "10")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].title", equalTo("청년 농업인 지원사업")))
            .andExpect(jsonPath("$.data.items[0].agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.items[0].eligibilitySummary", equalTo("만 40세 미만")))
            .andExpect(jsonPath("$.data.items[0].benefitSummary", equalTo("월 100만원 지원")))
            .andExpect(jsonPath("$.data.items[0].applicationPeriodLabel", equalTo("2026-06-01 ~ 2026-06-30")))
            .andExpect(jsonPath("$.data.items[0].sourceUrl", equalTo("https://policy.example.com/1")))
            .andExpect(jsonPath("$.data.nextCursor", nullValue()))
            .andExpect(jsonPath("$.data.totalCount", equalTo(1)));
    }

    @Test
    @DisplayName("search posts returns post summaries")
    void searchPostsReturnsPostSummaries() throws Exception {
        when(
            searchService.searchPosts(
                new SearchQuery(memberId, "황기", null, 20)
            )
        ).thenReturn(
            new SearchResult.PostPage(List.of(postSummary()), "cursor-2", 5)
        );

        mockMvc.perform(
            get("/api/v1/search/posts")
                .with(authenticatedMember(memberId.toString()))
                .param("keyword", "황기")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].title", equalTo("황기 수확 후기")))
            .andExpect(jsonPath("$.data.items[0].cropName", equalTo("황기")))
            .andExpect(jsonPath("$.data.items[0].commentCount", equalTo(2)))
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-2")))
            .andExpect(jsonPath("$.data.totalCount", equalTo(5)));
    }

    @Test
    @DisplayName("search endpoints without auth return unauthorized")
    void searchEndpointsWithoutAuthReturnUnauthorized() throws Exception {
        for (String path : List.of("/api/v1/search", "/api/v1/search/records", "/api/v1/search/policies", "/api/v1/search/posts")) {
                mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
            }
    }

    @Test
    @DisplayName("suggestions returns keywords from suggestion service")
    void suggestionsReturnsKeywordsFromSuggestionService() throws Exception {
        when(searchSuggestionService.suggest("황기"))
            .thenReturn(new SearchSuggestionResult.Suggestions(List.of("황기", "황기환", "황기차")));

        mockMvc.perform(
            get("/api/v1/search/suggestions")
                .with(authenticatedMember(memberId.toString()))
                .param("keyword", "황기")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.keywords[0]", equalTo("황기")))
            .andExpect(jsonPath("$.data.keywords.length()", equalTo(3)));
    }

    @Test
    @DisplayName("suggestions without auth returns unauthorized")
    void suggestionsWithoutAuthReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/search/suggestions").param("keyword", "황기"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_001")));
    }

    private FarmingRecordResult.Summary recordSummary() {
        return new FarmingRecordResult.Summary(
            recordId,
            cropId,
            "황기",
            WorkType.HARVEST,
            createdAt,
            "맑음",
            23,
            "수확 완료",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }

    private CommunityPostResult.PostSummary postSummary() {
        return new CommunityPostResult.PostSummary(
        postId,
        cropId,
        "황기",
        CommunityPostType.GENERAL,
        "황기 수확 후기",
        "올해 수확량이",
        null,
        new CommunityPostResult.AuthorSummary(
            memberId,
            "농부",
            null),
        2,
        5,
        false,
        createdAt);
    }

    private SearchResult.PolicyItem policyItem() {
        return new SearchResult.PolicyItem(
        policyId,
        "청년 농업인 지원사업",
        "농림축산식품부",
        "만 40세 미만",
        "월 100만원 지원",
        "2026-06-01 ~ 2026-06-30",
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        "https://policy.example.com/1",
        createdAt);
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(memberId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }
}
