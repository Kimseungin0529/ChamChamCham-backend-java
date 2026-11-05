package com.chamchamcham.application.auth.common;

import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

class OnboardingStatusResolverTest {

    @Test
    @DisplayName("resolve returns missing fields for incomplete member")
    void resolve_returns_missing_fields_for_incomplete_member() throws Exception {
        var member = new Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null);
        member.setPhone(null);
        member.setName(null);
        member.setBirthDate(null);
        member.setNickname(null);
        member.setExperienceLevel(null);
        member.setManagementType(null);

        var result = new OnboardingStatusResolver().resolve(member);
        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.getStatus());
        assertEquals(java.util.Arrays.asList(AuthResult.OnboardingField.EXPERIENCE_LEVEL, AuthResult.OnboardingField.MANAGEMENT_TYPE), result.getMissingFields());
    }

    @Test
    @DisplayName("resolve is complete even when optional profile fields are absent")
    void resolve_is_complete_even_when_optional_profile_fields_are_absent() throws Exception {
        var member = new Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null);
        member.setPhone(null);
        member.setName(null);
        member.setBirthDate(null);
        member.setNickname(null);
        member.setExperienceLevel(3);
        member.setManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);

        var result = new OnboardingStatusResolver().resolve(member);
        assertEquals(AuthResult.OnboardingStatus.COMPLETE, result.getStatus());
        assertTrue(result.getMissingFields().isEmpty());
    }

    @Test
    @DisplayName("resolve returns complete when all required onboarding fields exist")
    void resolve_returns_complete_when_all_required_onboarding_fields_exist() throws Exception {
        var member = new Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null);
        member.setPhone("010-1234-5678");
        member.setName("홍길동");
        member.setBirthDate(LocalDate.of(1998, 3, 12));
        member.setNickname("농부");
        member.setExperienceLevel(72);
        member.setManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);

        var result = new OnboardingStatusResolver().resolve(member);
        assertEquals(AuthResult.OnboardingStatus.COMPLETE, result.getStatus());
        assertTrue(result.getMissingFields().isEmpty());
    }

    @Test
    @DisplayName("resolve requires management type")
    void resolve_requires_management_type() throws Exception {
        var member = new Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null);
        member.setPhone("010-1234-5678");
        member.setName("홍길동");
        member.setBirthDate(LocalDate.of(1998, 3, 12));
        member.setNickname("농부");
        member.setExperienceLevel(72);
        member.setManagementType(null);

        var result = new OnboardingStatusResolver().resolve(member);
        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.getStatus());
        assertEquals(java.util.Arrays.asList(AuthResult.OnboardingField.MANAGEMENT_TYPE), result.getMissingFields());
    }
}
