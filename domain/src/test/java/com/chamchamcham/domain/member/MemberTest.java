package com.chamchamcham.domain.member;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MemberTest {
    @Test
    void socialMemberCanBeCreatedWithoutEmail() {
        var member = new Member(
            null, null, null, MemberStatus.ACTIVE, null, null, null, null,
            null, null, "ROLE_USER", null, null
        );

        assertNull(member.getEmail());
        assertNull(member.getPasswordHash());
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
    }

    @Test
    void completeOnboardingStoresProfileWithoutMemberRegion() {
        var member = new Member(
            UUID.fromString("00000000-0000-0000-0000-000000000001"), "member@example.com",
            null, MemberStatus.ACTIVE, null, null, null, null, null, null, "ROLE_USER", null, null
        );

        member.completeOnboarding(
            "홍길동", "010-1234-5678", LocalDate.of(1990, 1, 1), "길동", 72,
            ManagementType.AGRICULTURAL_INDIVIDUAL
        );

        assertEquals("홍길동", member.getName());
        assertEquals("010-1234-5678", member.getPhone());
        assertEquals(LocalDate.of(1990, 1, 1), member.getBirthDate());
        assertEquals("길동", member.getNickname());
        assertEquals(72, member.getExperienceLevel());
        assertEquals(ManagementType.AGRICULTURAL_INDIVIDUAL, member.getManagementType());
    }

    @Test
    void prefillProfileFillsMissingBlankNamePhoneAndNullBirthDate() {
        var member = member(" ", "", null);

        member.prefillProfile("홍길동", "010-1234-5678", LocalDate.of(1990, 5, 10));

        assertEquals("홍길동", member.getName());
        assertEquals("010-1234-5678", member.getPhone());
        assertEquals(LocalDate.of(1990, 5, 10), member.getBirthDate());
    }

    @Test
    void prefillProfilePreservesExistingNonblankNamePhoneAndExistingBirthDate() {
        var member = member("기존 이름", "010-0000-0000", LocalDate.of(1980, 1, 1));

        member.prefillProfile("홍길동", "010-1234-5678", LocalDate.of(1990, 5, 10));

        assertEquals("기존 이름", member.getName());
        assertEquals("010-0000-0000", member.getPhone());
        assertEquals(LocalDate.of(1980, 1, 1), member.getBirthDate());
    }

    private Member member(String name, String phone, LocalDate birthDate) {
        return new Member(
            null, "member@example.com", phone, MemberStatus.ACTIVE, name, birthDate,
            null, null, null, null, "ROLE_USER", null, null
        );
    }
}
