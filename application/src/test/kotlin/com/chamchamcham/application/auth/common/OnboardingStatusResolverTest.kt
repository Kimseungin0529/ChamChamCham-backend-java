package com.chamchamcham.application.auth.common

import com.chamchamcham.domain.member.ManagementType
import com.chamchamcham.domain.member.Member
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OnboardingStatusResolverTest {
    @Test
    fun `resolve returns missing fields for incomplete member`() {
        val member = Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null).also { migratedMember ->
            migratedMember.phone = null
            migratedMember.name = null
            migratedMember.birthDate = null
            migratedMember.nickname = null
            migratedMember.experienceLevel = null
            migratedMember.managementType = null
        }

        val result = OnboardingStatusResolver().resolve(member)

        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.status)
        assertEquals(
            listOf(
                AuthResult.OnboardingField.EXPERIENCE_LEVEL,
                AuthResult.OnboardingField.MANAGEMENT_TYPE
            ),
            result.missingFields
        )
    }

    @Test
    fun `resolve is complete even when optional profile fields are absent`() {
        val member = Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null).also { migratedMember ->
            migratedMember.phone = null
            migratedMember.name = null
            migratedMember.birthDate = null
            migratedMember.nickname = null
            migratedMember.experienceLevel = 3
            migratedMember.managementType = ManagementType.AGRICULTURAL_INDIVIDUAL
        }

        val result = OnboardingStatusResolver().resolve(member)

        assertEquals(AuthResult.OnboardingStatus.COMPLETE, result.status)
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `resolve returns complete when all required onboarding fields exist`() {
        val member = Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null).also { migratedMember ->
            migratedMember.phone = "010-1234-5678"
            migratedMember.name = "홍길동"
            migratedMember.birthDate = LocalDate.of(1998, 3, 12)
            migratedMember.nickname = "농부"
            migratedMember.experienceLevel = 72
            migratedMember.managementType = ManagementType.AGRICULTURAL_INDIVIDUAL
        }

        val result = OnboardingStatusResolver().resolve(member)

        assertEquals(AuthResult.OnboardingStatus.COMPLETE, result.status)
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `resolve requires management type`() {
        val member = Member(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, null).also { migratedMember ->
            migratedMember.phone = "010-1234-5678"
            migratedMember.name = "홍길동"
            migratedMember.birthDate = LocalDate.of(1998, 3, 12)
            migratedMember.nickname = "농부"
            migratedMember.experienceLevel = 72
            migratedMember.managementType = null
        }

        val result = OnboardingStatusResolver().resolve(member)

        assertEquals(AuthResult.OnboardingStatus.REQUIRED, result.status)
        assertEquals(listOf(AuthResult.OnboardingField.MANAGEMENT_TYPE), result.missingFields)
    }
}
