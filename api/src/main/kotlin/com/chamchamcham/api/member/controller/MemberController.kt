package com.chamchamcham.api.member.controller

import com.chamchamcham.api.common.ApiResponse
import com.chamchamcham.api.member.dto.MemberRequests
import com.chamchamcham.api.member.dto.MemberResponses
import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.application.member.MemberProfileCommand
import com.chamchamcham.application.member.MemberProfileService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberProfileService: MemberProfileService
) {
    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal memberId: String?
    ): ResponseEntity<ApiResponse<MemberResponses.MyProfileResponse>> {
        val profile = memberProfileService.getMyProfile(parseMemberId(memberId))
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.MyProfileResponse.from(profile)))
    }

    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal memberId: String?
    ): ResponseEntity<ApiResponse<Unit>> {
        memberProfileService.withdraw(parseMemberId(memberId))
        return ResponseEntity.ok(ApiResponse.empty(Unit))
    }

    @GetMapping("/me/farm-crops")
    fun getMyFarmCrops(
        @AuthenticationPrincipal memberId: String?
    ): ResponseEntity<ApiResponse<List<MemberResponses.FarmCropsResponse>>> {
        val farmCrops = memberProfileService.getMyFarmCrops(parseMemberId(memberId))
        return ResponseEntity.ok(ApiResponse.ok(farmCrops.map(MemberResponses.FarmCropsResponse::from)))
    }

    @PutMapping("/me/profile")
    fun updateMyProfile(
        @AuthenticationPrincipal memberId: String?,
        @Valid @RequestBody request: MemberRequests.UpdateMyProfileRequest
    ): ResponseEntity<ApiResponse<MemberResponses.MyProfileResponse>> {
        val profile = memberProfileService.updateMyProfile(
            MemberProfileCommand.UpdateMyProfile(
                parseMemberId(memberId),
                request.name,
                request.phone,
                request.birthDate,
                request.nickname,
                requireNotNull(request.experienceLevel),
                requireNotNull(request.managementType),
                request.profileMediaId,
                request.farms.map { farm ->
                    MemberProfileCommand.Farm(
                        farm.farmId,
                        farm.name,
                        farm.roadAddress,
                        farm.jibunAddress,
                        farm.latitude,
                        farm.longitude,
                        farm.pnu,
                        farm.landCategory,
                        farm.areaSqm,
                        farm.areaIsManualEntry,
                        farm.boundaryCoordinates.map {
                            MemberProfileCommand.FarmBoundaryCoordinate(
                                requireNotNull(it.latitude),
                                requireNotNull(it.longitude)
                            )
                        },
                        MemberProfileCommand.FarmDataSource(
                            farm.dataSource.address,
                            farm.dataSource.coordinate,
                            farm.dataSource.parcel,
                            farm.dataSource.landCharacteristic
                        ),
                        farm.cropIds
                    )
                }
            )
        )
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.MyProfileResponse.from(profile)))
    }

    @GetMapping("/{memberId}/profile")
    fun getPublicProfile(
        @AuthenticationPrincipal authenticatedMemberId: String?,
        @PathVariable memberId: UUID
    ): ResponseEntity<ApiResponse<MemberResponses.PublicProfileResponse>> {
        parseMemberId(authenticatedMemberId)
        val profile = memberProfileService.getPublicProfile(memberId)
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.PublicProfileResponse.from(profile)))
    }

    private fun parseMemberId(memberId: String?): UUID {
        if (memberId.isNullOrBlank()) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }

        return try {
            UUID.fromString(memberId)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }
    }
}
