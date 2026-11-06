package com.chamchamcham.api.search;

import com.chamchamcham.api.pesticide.controller.AdminPesticideSyncController;
import com.chamchamcham.api.pesticide.dto.PesticideResponses;
import com.chamchamcham.api.policy.controller.AdminPolicySyncController;
import com.chamchamcham.api.policy.controller.PolicyController;
import com.chamchamcham.api.policy.dto.program.PolicyProgramDetailResponse;
import com.chamchamcham.api.policy.dto.recommendation.PolicyRecommendationPageResponse;
import com.chamchamcham.api.policy.dto.sync.PolicySyncJobDetailResponse;
import com.chamchamcham.api.search.controller.SearchController;
import com.chamchamcham.api.search.dto.SearchResponses;
import com.chamchamcham.api.search.dto.SearchSuggestionResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.pesticide.PesticideResult;
import com.chamchamcham.application.pesticide.sync.PesticideSyncService;
import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import com.chamchamcham.application.policy.recommendation.PolicyRecommendationService;
import com.chamchamcham.application.policy.sync.PolicySyncAsyncRunner;
import com.chamchamcham.application.policy.sync.PolicySyncResult;
import com.chamchamcham.application.policy.sync.PolicySyncService;
import com.chamchamcham.application.search.SearchResult;
import com.chamchamcham.application.search.SearchService;
import com.chamchamcham.application.search.SearchSuggestionResult;
import com.chamchamcham.application.search.SearchSuggestionService;
import com.chamchamcham.domain.policy.PolicySource;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import com.chamchamcham.domain.policy.PolicySyncTriggerType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CatalogSearchContractTest {
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final LocalDateTime time = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void mappedPagesRetainOrderAndMutableListsAtEveryCardinality() {
        var a = new PesticideResult.Summary(id, "성분1", "상표1", null, null);
        var b = new PesticideResult.Summary(UUID.fromString("00000000-0000-0000-0000-000000000002"), "성분2", "상표2", "살충", "제조사");
        var firstCard = card("첫 정책");
        var secondCard = card("둘째 정책");
        var firstPolicy = policy("첫 정책");
        var secondPolicy = policy("둘째 정책");
        for (int size : new int[]{0, 1, 2}) {
            var pesticides = PesticideResponses.PesticidePageResponse.from(new PesticideResult.Page(List.of(a, b).subList(0, size), null));
            assertThat(pesticides.getItems()).extracting(PesticideResponses.PesticideSummaryResponse::getItemName)
                .containsExactlyElementsOf(List.of("성분1", "성분2").subList(0, size));
            pesticides.getItems().add(PesticideResponses.PesticideSummaryResponse.from(a));
            assertThat(pesticides.getItems()).hasSize(size + 1);
            var recommendations = PolicyRecommendationPageResponse.from(new PolicyRecommendationResult.Page(List.of(firstCard, secondCard).subList(0, size), null));
            assertThat(recommendations.getItems()).extracting(item -> item.getProgramTitle())
                .containsExactlyElementsOf(List.of("첫 정책", "둘째 정책").subList(0, size));
            recommendations.getItems().add(recommendations.getItems().isEmpty()
                ? PolicyRecommendationPageResponse.from(new PolicyRecommendationResult.Page(List.of(firstCard), null)).getItems().get(0)
                : recommendations.getItems().get(0));
            assertThat(recommendations.getItems()).hasSize(size + 1);
            var policies = SearchResponses.SearchPolicyPageResponse.from(new SearchResult.PolicyPage(List.of(firstPolicy, secondPolicy).subList(0, size), null, size));
            assertThat(policies.getItems()).extracting(SearchResponses.SearchPolicyItemResponse::getTitle)
                .containsExactlyElementsOf(List.of("첫 정책", "둘째 정책").subList(0, size));
            policies.getItems().add(SearchResponses.SearchPolicyItemResponse.from(firstPolicy));
            assertThat(policies.getItems()).hasSize(size + 1);
        }
    }

    @Test
    void suggestionResponseKeepsTheOriginalKeywordListReference() {
        var keywords = new ArrayList<>(List.of("황기", "황기차"));
        var response = SearchSuggestionResponses.SuggestionsResponse.from(new SearchSuggestionResult.Suggestions(keywords));
        assertThat(response.getKeywords()).isSameAs(keywords);
        keywords.add("황기환");
        assertThat(response.getKeywords()).containsExactly("황기", "황기차", "황기환");
    }

    @Test
    void responseConstructorsKeepListAliasesAndValueEquality() {
        var items = new ArrayList<PesticideResponses.PesticideSummaryResponse>();
        var page = new PesticideResponses.PesticidePageResponse(items, null);
        assertThat(page.getItems()).isSameAs(items);
        var item = new PesticideResponses.PesticideSummaryResponse(id, "성분", "상표", null, null);
        items.add(item);
        assertThat(page.getItems()).containsExactly(item);
        assertThat(item).isEqualTo(new PesticideResponses.PesticideSummaryResponse(id, "성분", "상표", null, null));
        assertThatThrownBy(() -> new PesticideResponses.PesticideSummaryResponse(null, "성분", "상표", null, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canonicalJsonIncludesNullableFieldsAndOriginalDateAndNumericValues() throws Exception {
        var pesticide = PesticideResponses.PesticideSummaryResponse.from(new PesticideResult.Summary(id, "성분", "상표", null, null));
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(pesticide)).isEqualTo(mapper.readTree("""
            {"id":"00000000-0000-0000-0000-000000000001","itemName":"성분","brandName":"상표","usageCategory":null,"manufacturer":null}
            """));
        var detail = PolicyProgramDetailResponse.from(detail());
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(detail)).isEqualTo(mapper.readTree("""
            {"id":"00000000-0000-0000-0000-000000000001","programTitle":"정책","sourceYear":"2026","agencyName":"기관",
             "departmentName":null,"applicationPeriodLabel":"상시","onlineApplyAvailable":false,"sourceUrl":null,"applicationUrl":null,
             "purpose":null,"summary":null,"eligibility":null,"benefit":null,"applicationMethod":null,"requiredDocuments":null,"selectionCriteria":null,
             "contacts":[{"agencyName":null,"departmentName":null,"phoneNumber":null}],"attachments":[{"fileName":null,"extension":null,"sizeBytes":null,"url":null}]}
            """));
        var job = PolicySyncJobDetailResponse.from(new PolicySyncResult.JobDetail(id, PolicySource.NONGUP_EZ,
            "2026", PolicySyncTriggerType.ADMIN, PolicySyncJobStatus.RUNNING, 1, 2, 3, 4, null, time, null));
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(job)).isEqualTo(mapper.readTree("""
            {"jobId":"00000000-0000-0000-0000-000000000001","source":"NONGUP_EZ","targetYear":"2026","triggerType":"ADMIN","status":"RUNNING",
             "totalCount":1,"syncedCount":2,"detailSuccessCount":3,"detailFailureCount":4,"errorMessage":null,"startedAt":"2026-01-01T09:00:00","finishedAt":null}
            """));
        var search = SearchResponses.SearchPolicyItemResponse.from(policy("정책"));
        assertThat(mapper.<com.fasterxml.jackson.databind.JsonNode>valueToTree(search)).isEqualTo(mapper.readTree("""
            {"id":"00000000-0000-0000-0000-000000000001","title":"정책","agencyName":"기관","eligibilitySummary":"대상","benefitSummary":"혜택",
             "applicationPeriodLabel":"상시","applyStartsOn":"2026-01-01","applyEndsOn":null,"sourceUrl":null,"createdAt":"2026-01-01T09:00:00"}
            """));
    }

    @Test
    void completedAdminJobAcceptsUserDetailsAndDoesNotRestartTheRunner() {
        var service = mock(PolicySyncService.class);
        var runner = mock(PolicySyncAsyncRunner.class);
        when(service.createAdminSyncJob(id)).thenReturn(new PolicySyncResult.JobSummary(id, PolicySyncJobStatus.SUCCEEDED, "2026"));
        var principal = User.withUsername(id.toString()).password("unused").roles("ADMIN").build();
        var response = new AdminPolicySyncController(service, runner).createJob(principal);
        assertThat(response.getBody().getData().getStatus()).isEqualTo(PolicySyncJobStatus.SUCCEEDED);
        verify(service).createAdminSyncJob(id);
        verifyNoInteractions(runner);
    }

    @Test
    void invalidPrincipalsAreRejectedBeforeServicesAreCalled() {
        var recommendationService = mock(PolicyRecommendationService.class);
        var syncService = mock(PolicySyncService.class);
        var runner = mock(PolicySyncAsyncRunner.class);
        var searchService = mock(SearchService.class);
        var suggestions = mock(SearchSuggestionService.class);
        var policyController = new PolicyController(recommendationService);
        var adminController = new AdminPolicySyncController(syncService, runner);
        var searchController = new SearchController(searchService, suggestions);
        for (Object principal : new Object[]{null, "", " \u00a0", "malformed", new Object()}) {
            assertThatThrownBy(() -> policyController.getProgramDetail(principal, id)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
            assertThatThrownBy(() -> adminController.createJob(principal)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
        for (String principal : new String[]{null, "", " \u00a0", "malformed"}) {
            assertThatThrownBy(() -> searchController.suggestions(principal, "황기")).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        }
        verifyNoInteractions(recommendationService, syncService, runner, searchService, suggestions);
    }

    @Test
    void recommendationFiltersAreValidatedBeforeAuthentication() {
        var service = mock(PolicyRecommendationService.class);
        var controller = new PolicyController(service);
        assertThatThrownBy(() -> controller.listRecommendations(null, null, 20, "BAD", "RECOMMENDED"))
            .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> controller.listRecommendations(null, null, 20, null, "BAD"))
            .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verifyNoInteractions(service);
    }

    @Test
    void synchronousPesticideFailureCrossesTheControllerUnchanged() throws Exception {
        var service = mock(PesticideSyncService.class);
        var failure = new Exception("sync fixture failure");
        when(service.sync()).thenThrow(failure);
        assertThatThrownBy(() -> new AdminPesticideSyncController(service).sync()).isSameAs(failure);
    }

    private PolicyRecommendationResult.Card card(String title) {
        return new PolicyRecommendationResult.Card(id, id, title, "대상", "혜택", "상시", "기관", new BigDecimal("87.50"), "이유", time);
    }

    private SearchResult.PolicyItem policy(String title) {
        return new SearchResult.PolicyItem(id, title, "기관", "대상", "혜택", "상시", LocalDate.of(2026, 1, 1), null, null, time);
    }

    private PolicyRecommendationResult.Detail detail() {
        return new PolicyRecommendationResult.Detail(id, "정책", "2026", "기관", null, "상시", false, null, null,
            null, null, null, null, null, null, null, List.of(new PolicyRecommendationResult.Contact(null, null, null)),
            List.of(new PolicyRecommendationResult.Attachment(null, null, null, null)));
    }
}
