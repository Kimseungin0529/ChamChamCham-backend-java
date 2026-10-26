package com.chamchamcham.api

import com.chamchamcham.api.auth.dto.AuthRequests
import com.chamchamcham.api.member.dto.MemberRequests
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ExperienceLevelValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `onboarding range error does not also report age error`() {
        val request = AuthRequests.CompleteOnboardingRequest(
            birthDate = LocalDate.now().minusYears(30),
            experienceLevel = 101,
            managementType = null,
            farm = null,
            cropIds = emptyList<UUID>()
        )

        val fields = validator.validate(request)
            .map { it.propertyPath.toString() }
            .filter { it.startsWith("experienceLevel") }

        assertThat(fields).containsExactly("experienceLevel")
    }

    @Test
    fun `profile range error does not also report age error`() {
        val request = MemberRequests.UpdateMyProfileRequest(
            birthDate = LocalDate.now().minusYears(30),
            experienceLevel = 101,
            managementType = null,
            farms = emptyList()
        )

        val fields = validator.validate(request)
            .map { it.propertyPath.toString() }
            .filter { it.startsWith("experienceLevel") }

        assertThat(fields).containsExactly("experienceLevel")
    }
}
