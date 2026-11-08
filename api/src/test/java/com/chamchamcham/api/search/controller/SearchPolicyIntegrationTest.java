package com.chamchamcham.api.search.controller;

import com.chamchamcham.api.testsupport.TestJwtFixtures;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import com.chamchamcham.domain.policy.PolicySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.mail.username=test@example.com", "spring.mail.password=test-password"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchPolicyIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PolicyProgramRepository policyProgramRepository;
    private String accessToken;
    private String programAId;

    @BeforeEach
    void setUp() {
        var member = memberRepository.save(new Member("policy-search-member@example.com", null));
        accessToken = TestJwtFixtures.signedTestToken(
            member.getId() == null ? "null" : member.getId().toString(), "access", "ROLE_USER");
        var programA = policyProgramRepository.save(buildProgram(
            "청년 농업인 지원사업", "청년 농업인 정착 지원", "월 100만원 지원",
            "policy-search-a", "https://policy.example.com/a", true));
        policyProgramRepository.save(buildProgram(
            "스마트팜 구축 지원", "스마트팜 시설 구축 보조", "시설비 50% 지원",
            "policy-search-b", "https://policy.example.com/b", true));
        policyProgramRepository.save(buildProgram(
            "청년 미동기화 정책", "상세 수집 실패 건", "지원 확인",
            "policy-search-c", null, false));
        programAId = Objects.requireNonNull(programA.getId()).toString();
    }

    @Test
    @DisplayName("policy search with matching keyword returns only the synced matching program with card fields")
    void policySearchWithMatchingKeywordReturnsOnlyTheSyncedMatchingProgramWithCardFields() throws Exception {
        mockMvc.perform(get("/api/v1/search/policies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).param("keyword", "청년"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.items.length()", equalTo(1)))
            .andExpect(jsonPath("$.data.items[0].id", equalTo(programAId)))
            .andExpect(jsonPath("$.data.items[0].title", equalTo("청년 농업인 지원사업")))
            .andExpect(jsonPath("$.data.items[0].agencyName", equalTo("농림축산식품부")))
            .andExpect(jsonPath("$.data.items[0].eligibilitySummary", equalTo("자격 확인")))
            .andExpect(jsonPath("$.data.items[0].benefitSummary", equalTo("월 100만원 지원")))
            .andExpect(jsonPath("$.data.items[0].applicationPeriodLabel", equalTo("접수기관문의")))
            .andExpect(jsonPath("$.data.items[0].sourceUrl", equalTo("https://policy.example.com/a")))
            .andExpect(jsonPath("$.data.totalCount", equalTo(1)));
    }

    @Test
    @DisplayName("policy search with non matching keyword returns no items")
    void policySearchWithNonMatchingKeywordReturnsNoItems() throws Exception {
        mockMvc.perform(get("/api/v1/search/policies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).param("keyword", "존재하지않는키워드"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()", equalTo(0)))
            .andExpect(jsonPath("$.data.totalCount", equalTo(0)));
    }

    @Test
    @DisplayName("policy search without keyword returns every synced program")
    void policySearchWithoutKeywordReturnsEverySyncedProgram() throws Exception {
        mockMvc.perform(get("/api/v1/search/policies").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()", equalTo(2)))
            .andExpect(jsonPath("$.data.totalCount", equalTo(2)));
    }

    @Test
    @DisplayName("search all includes a policies section with the seeded program")
    void searchAllIncludesAPoliciesSectionWithTheSeededProgram() throws Exception {
        mockMvc.perform(get("/api/v1/search").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records.totalCount", equalTo(0)))
            .andExpect(jsonPath("$.data.posts.totalCount", equalTo(0)))
            .andExpect(jsonPath("$.data.policies.totalCount", equalTo(2)))
            .andExpect(jsonPath("$.data.policies.items[*].title", hasItem("청년 농업인 지원사업")));
    }

    private PolicyProgram buildProgram(String title, String summary, String benefitSummary,
                                       String externalId, String sourceUrl, boolean detailSynced) {
        var program = new PolicyProgram(title, "정책 상세", "전국", null);
        program.applyListFields(PolicySource.NONGUP_EZ, externalId, "2026", title, summary, "전국", sourceUrl, "농림축산식품부");
        if (detailSynced) {
            program.applyDetailFields("정책 상세", null, null, "자격 확인", null, benefitSummary,
                null, null, "접수기관문의", null, null, null, null, null, false, null,
                "[]", "[]", "[]", "{}", true);
        }
        return program;
    }
}
