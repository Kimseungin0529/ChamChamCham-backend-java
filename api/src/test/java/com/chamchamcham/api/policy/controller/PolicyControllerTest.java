package com.chamchamcham.api.policy.controller;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.policy.recommendation.PolicyRecommendationResult;
import com.chamchamcham.application.policy.recommendation.PolicyRecommendationService;
import com.chamchamcham.application.policy.support.PolicyBenefitCategory;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.policy.PolicyRecommendationSort;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@WebMvcTest(PolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PolicyControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID recommendationId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID policyProgramId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @MockBean
    private PolicyRecommendationService policyRecommendationService;

    @MockBean
    private TokenProvider tokenProvider;


    @Test
    @DisplayName("list recommendations maps authenticated principal and returns items plus next cursor")
    void listRecommendationsMapsAuthenticatedPrincipalAndReturnsItemsPlusNextCursor() throws Exception {
        when(
            policyRecommendationService.listRecommendations(
                memberId,
                null,
                20,
                null,
                PolicyRecommendationSort.RECOMMENDED
            )
        )
            .thenReturn(
                new PolicyRecommendationResult.Page(
                    List.of(
                        new PolicyRecommendationResult.Card(
                            recommendationId,
                            policyProgramId,
                            "청년농 정착 지원",
                            "만 40세 미만",
                            "월 최대 110만원",
                            "2026.01.01 - 2026.02.28",
                            "농림축산식품부",
                            new BigDecimal("87.50"),
                            "재배 작물과 지역 조건이 일치합니다",
                            LocalDateTime.of(2026, 1, 1, 0, 0)
                        )
                    ),
                    "cursor-2"
                )
            );

        mockMvc.perform(
            get("/api/v1/policies/recommendations")
                .param("size", "20")
                .with(authenticatedMember(memberId.toString()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].recommendationId", equalTo(recommendationId.toString())))
            .andExpect(jsonPath("$.data.items[0].policyProgramId", equalTo(policyProgramId.toString())))
            .andExpect(jsonPath("$.data.items[0].programTitle", equalTo("청년농 정착 지원")))
            .andExpect(jsonPath("$.data.items[0].eligibilitySummary", equalTo("만 40세 미만")))
            .andExpect(jsonPath("$.data.items[0].benefitSummary", equalTo("월 최대 110만원")))
            .andExpect(jsonPath("$.data.items[0].applicationPeriodLabel", equalTo("2026.01.01 - 2026.02.28")))
            .andExpect(jsonPath("$.data.items[0].agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.items[0].score", equalTo(87.50)))
            .andExpect(jsonPath("$.data.items[0].reason", equalTo("재배 작물과 지역 조건이 일치합니다")))
            .andExpect(jsonPath("$.data.nextCursor", equalTo("cursor-2")));

        verify(policyRecommendationService).listRecommendations(
            memberId,
            null,
            20,
            null,
            PolicyRecommendationSort.RECOMMENDED
        );
    }

    @Test
    @DisplayName("list recommendations passes benefit category filter to service")
    void listRecommendationsPassesBenefitCategoryFilterToService() throws Exception {
        when(
            policyRecommendationService.listRecommendations(
                memberId,
                null,
                20,
                PolicyBenefitCategory.FINANCE,
                PolicyRecommendationSort.RECOMMENDED
            )
        ).thenReturn(new PolicyRecommendationResult.Page(List.of(), null));

        mockMvc.perform(
            get("/api/v1/policies/recommendations")
                .param("benefitCategory", "FINANCE")
                .with(authenticatedMember(memberId.toString()))
        )
            .andExpect(status().isOk());

        verify(policyRecommendationService).listRecommendations(
            memberId,
            null,
            20,
            PolicyBenefitCategory.FINANCE,
            PolicyRecommendationSort.RECOMMENDED
        );
    }

    @Test
    @DisplayName("list recommendations passes latest sort to service")
    void listRecommendationsPassesLatestSortToService() throws Exception {
        when(
            policyRecommendationService.listRecommendations(
                memberId,
                null,
                20,
                null,
                PolicyRecommendationSort.LATEST
            )
        ).thenReturn(new PolicyRecommendationResult.Page(List.of(), null));

        mockMvc.perform(
            get("/api/v1/policies/recommendations")
                .param("sort", "LATEST")
                .with(authenticatedMember(memberId.toString()))
        ).andExpect(status().isOk());

        verify(policyRecommendationService).listRecommendations(
            memberId,
            null,
            20,
            null,
            PolicyRecommendationSort.LATEST
        );
    }

    @Test
    @DisplayName("list recommendations rejects unknown sort")
    void listRecommendationsRejectsUnknownSort() throws Exception {
        mockMvc.perform(
            get("/api/v1/policies/recommendations")
                .param("sort", "BAD")
                .with(authenticatedMember(memberId.toString()))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo(ErrorCode.INVALID_INPUT.getCode())));

        verifyNoInteractions(policyRecommendationService);
    }

    @Test
    @DisplayName("list recommendations rejects unknown benefit category")
    void listRecommendationsRejectsUnknownBenefitCategory() throws Exception {
        mockMvc.perform(
            get("/api/v1/policies/recommendations")
                .param("benefitCategory", "BAD")
                .with(authenticatedMember(memberId.toString()))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", equalTo(ErrorCode.INVALID_INPUT.getCode())));

        verifyNoInteractions(policyRecommendationService);
    }

    @Test
    @DisplayName("get program detail returns detail sections contacts and attachments")
    void getProgramDetailReturnsDetailSectionsContactsAndAttachments() throws Exception {
        when(policyRecommendationService.getProgramDetail(memberId, policyProgramId))
            .thenReturn(
                new PolicyRecommendationResult.Detail(
                    policyProgramId,
                    "청년농 정착 지원",
                    "2026",
                    "농림축산식품부",
                    "청년농육성팀",
                    "2026.01.01 - 2026.02.28",
                    true,
                    "https://www.nongupez.go.kr/detail",
                    "https://www.nongupez.go.kr/apply",
                    "영농 초기 정착 지원",
                    "청년농을 위한 정착 지원",
                    "만 40세 미만 농업인",
                    "정착지원금",
                    "온라인 신청",
                    "사업계획서",
                    "영농 의지",
                    List.of(
                        new PolicyRecommendationResult.Contact(
                            "농림축산식품부",
                            "청년농육성팀",
                            "044-000-0000"
                        )
                    ),
                    List.of(
                        new PolicyRecommendationResult.Attachment(
                            "guide.pdf",
                            "pdf",
                            1024L,
                            "https://www.nongupez.go.kr/guide.pdf"
                        )
                    )
                )
            );

        mockMvc.perform(
            get("/api/v1/policies/{policyProgramId}", policyProgramId)
                .with(authenticatedMember(memberId.toString()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", equalTo(policyProgramId.toString())))
            .andExpect(jsonPath("$.data.programTitle", equalTo("청년농 정착 지원")))
            .andExpect(jsonPath("$.data.eligibility", equalTo("만 40세 미만 농업인")))
            .andExpect(jsonPath("$.data.benefit", equalTo("정착지원금")))
            .andExpect(jsonPath("$.data.applicationPeriodLabel", equalTo("2026.01.01 - 2026.02.28")))
            .andExpect(jsonPath("$.data.agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.contacts[0].agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.contacts[0].departmentName", equalTo("청년농육성팀")))
            .andExpect(jsonPath("$.data.contacts[0].phoneNumber", equalTo("044-000-0000")))
            .andExpect(jsonPath("$.data.attachments[0].fileName", equalTo("guide.pdf")))
            .andExpect(jsonPath("$.data.attachments[0].extension", equalTo("pdf")))
            .andExpect(jsonPath("$.data.attachments[0].sizeBytes", equalTo(1024)))
            .andExpect(jsonPath("$.data.attachments[0].url", equalTo("https://www.nongupez.go.kr/guide.pdf")));

        verify(policyRecommendationService).getProgramDetail(memberId, policyProgramId);
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
