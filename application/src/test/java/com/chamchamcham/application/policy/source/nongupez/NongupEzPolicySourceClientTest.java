package com.chamchamcham.application.policy.source.nongupez;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;

class NongupEzPolicySourceClientTest {
    private static final String CONDITIONS_PATH = "/nsm/bizAply/wholeBiz/retrieveListBizSrchCnd";
    private static final String LIST_PATH = "/nsm/bizAply/wholeBiz/retrieveListBizSrch";
    private static final String DETAIL_PATH = "/nsm/bizAply/cstBiz/findBizSrchDtl";

    @Test
    @DisplayName("detectLatestYear ignores all option and selects max numeric year")
    void detectsLatestYear() {
        var client = new NongupEzPolicySourceClient(fakeTransport(CONDITIONS_PATH, fixture("conditions.json")));
        assertThat(client.detectLatestYear()).isEqualTo("2026");
    }

    @Test
    @DisplayName("fetchPrograms parses list page into source list item")
    void parsesListPage() {
        var captured = new AtomicReference<Map<String, ?>>();
        var client = new NongupEzPolicySourceClient((path, body) -> {
            captured.set(body);
            if (!path.equals(LIST_PATH)) throw new AssertionError(path);
            return fixture("list-page.json");
        });
        var programs = client.fetchPrograms("2026");
        assertThat(programs).hasSize(1);
        assertThat(programs.getFirst().getExternalId()).isEqualTo("AB000009");
        assertThat(programs.getFirst().getTitle()).isEqualTo("친환경농업직불");
        assertThat(programs.getFirst().getAgencyName()).isEqualTo("농림축산식품부");
        assertThat(programs.getFirst().getApplyEndsOn().toString()).isEqualTo("2026-06-30");
        var condition = (Map<?, ?>) captured.get().get("srchCnd");
        var paging = (Map<?, ?>) captured.get().get("paging");
        assertThat(condition.get("srchBizYr")).isEqualTo("2026");
        assertThat(condition.get("sortCnd")).isEqualTo("M");
        assertThat(paging.get("curPage")).isEqualTo(1);
        assertThat(paging.get("pageSize")).isEqualTo(1000);
    }

    @Test
    @DisplayName("fetchDetail parses long text contacts and attachments")
    void parsesDetail() {
        var client = new NongupEzPolicySourceClient(fakeTransport(DETAIL_PATH, fixture("detail-success.json")));
        var detail = client.fetchDetail("AB000009", "2026");
        assertThat(detail.getEligibility()).contains("농업경영정보");
        assertThat(detail.getContacts().getFirst().getDepartmentName()).isEqualTo("친환경농업과");
        assertThat(detail.getAttachments().getFirst().getFileName()).contains("시행지침");
    }

    @Test
    @DisplayName("fetchDetail tolerates null optional fields")
    void toleratesOptionalNulls() {
        var client = new NongupEzPolicySourceClient(fakeTransport(DETAIL_PATH, fixture("detail-missing-optional-fields.json")));
        var detail = client.fetchDetail("AB000010", "2026");
        assertThat(detail.getAgencyName()).isEqualTo("농업e지");
        assertThat(detail.getEligibility()).isNull();
        assertThat(detail.getBenefit()).isNull();
    }

    private NongupEzHttpTransport fakeTransport(String expectedPath, String response) {
        return (path, body) -> {
            if (!expectedPath.equals(path)) throw new AssertionError(path);
            return response;
        };
    }

    private String fixture(String name) {
        try (var input = getClass().getClassLoader().getResourceAsStream("policy/nongupez/" + name)) {
            return new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
}
