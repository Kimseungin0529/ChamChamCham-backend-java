package com.chamchamcham.config;

import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.api.policy.controller.AdminPolicySyncController;
import com.chamchamcham.api.security.CustomAccessDeniedHandler;
import com.chamchamcham.api.security.CustomAuthenticationEntryPoint;
import com.chamchamcham.application.policy.sync.PolicySyncAsyncRunner;
import com.chamchamcham.application.policy.sync.PolicySyncResult;
import com.chamchamcham.application.policy.sync.PolicySyncService;
import com.chamchamcham.application.security.TokenProvider;
import com.chamchamcham.domain.policy.PolicySyncJobStatus;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;


@WebMvcTest(AdminPolicySyncController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    MDCLoggingFilter.class,
    GlobalExceptionHandler.class
})
class SecurityConfigAdminTest {
    @Autowired private MockMvc mockMvc;
    private final UUID adminMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @MockBean
    private PolicySyncService policySyncService;

    @MockBean
    private PolicySyncAsyncRunner policySyncAsyncRunner;

    @MockBean
    private TokenProvider tokenProvider;

    @Test
    @DisplayName("unauthenticated admin request returns 401")
    void unauthenticatedAdminRequestReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/policies/sync-jobs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("authenticated user role admin request returns 403")
    void authenticatedUserRoleAdminRequestReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/policies/sync-jobs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = {"ADMIN"})
    @DisplayName("authenticated admin role admin request reaches controller")
    void authenticatedAdminRoleAdminRequestReachesController() throws Exception {
        when(policySyncService.createAdminSyncJob(adminMemberId))
            .thenReturn(
                new PolicySyncResult.JobSummary(
                    jobId,
                    PolicySyncJobStatus.SUCCEEDED,
                    "2026"
                )
            );

        mockMvc.perform(post("/api/v1/admin/policies/sync-jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.jobId", equalTo(jobId.toString())));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("access denied preserves error detail and UTF8 JSON")
    void accessDeniedPreservesJsonContract() throws Exception {
        mockMvc.perform(post("/api/v1/admin/policies/sync-jobs"))
            .andExpect(status().isForbidden())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.error.detail").value("You do not have permission to access this resource"));
    }


    @Test
    @DisplayName("credentialed CORS rejects Unicode blank origins and wildcard origins")
    void credentialedCorsRejectsBlankAndWildcardOrigins() {
        var denied = new CustomAccessDeniedHandler();
        var entry = new CustomAuthenticationEntryPoint();
        var jwt = org.mockito.Mockito.mock(com.chamchamcham.api.security.JwtAuthenticationFilter.class);
        for (String origins : java.util.List.of("\u00a0,\u2007", "https://*.example.test", "*")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> new SecurityConfig(denied, entry, jwt, new MDCLoggingFilter(), origins))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

}
