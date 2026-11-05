package com.chamchamcham.api.member.controller;

import com.chamchamcham.api.common.ApiResponse;
import com.chamchamcham.api.member.dto.MemberRequests;
import com.chamchamcham.api.member.dto.MemberResponses;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.member.MemberProfileCommand;
import com.chamchamcham.application.member.MemberProfileService;
import jakarta.validation.Valid;
import kotlin.Unit;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
    private final MemberProfileService memberProfileService;
    public MemberController(MemberProfileService memberProfileService) { this.memberProfileService = memberProfileService; }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponses.MyProfileResponse>> getMyProfile(@AuthenticationPrincipal String memberId) {
        var profile = memberProfileService.getMyProfile(parseMemberId(memberId));
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.MyProfileResponse.from(profile)));
    }
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Unit>> withdraw(@AuthenticationPrincipal String memberId) {
        memberProfileService.withdraw(parseMemberId(memberId));
        return ResponseEntity.ok(ApiResponse.empty(Unit.INSTANCE));
    }
    @GetMapping("/me/farm-crops")
    public ResponseEntity<ApiResponse<List<MemberResponses.FarmCropsResponse>>> getMyFarmCrops(@AuthenticationPrincipal String memberId) {
        var farmCrops = memberProfileService.getMyFarmCrops(parseMemberId(memberId));
        return ResponseEntity.ok(ApiResponse.ok(farmCrops.stream().map(MemberResponses.FarmCropsResponse::from).collect(Collectors.toList())));
    }
    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<MemberResponses.MyProfileResponse>> updateMyProfile(@AuthenticationPrincipal String memberId,
            @Valid @RequestBody MemberRequests.UpdateMyProfileRequest request) {
        var profile = memberProfileService.updateMyProfile(new MemberProfileCommand.UpdateMyProfile(parseMemberId(memberId),
            request.name(), request.phone(), request.birthDate(), request.nickname(), Objects.requireNonNull(request.experienceLevel()),
            Objects.requireNonNull(request.managementType()), request.profileMediaId(), request.farms().stream().map(farm -> new MemberProfileCommand.Farm(
                farm.farmId(), farm.name(), farm.roadAddress(), farm.jibunAddress(), farm.latitude(), farm.longitude(), farm.pnu(), farm.landCategory(),
                farm.areaSqm(), farm.areaIsManualEntry(), farm.boundaryCoordinates().stream().map(c -> new MemberProfileCommand.FarmBoundaryCoordinate(
                    Objects.requireNonNull(c.latitude()), Objects.requireNonNull(c.longitude()))).collect(Collectors.toList()),
                new MemberProfileCommand.FarmDataSource(farm.dataSource().address(), farm.dataSource().coordinate(), farm.dataSource().parcel(),
                    farm.dataSource().landCharacteristic()), farm.cropIds())).collect(Collectors.toList())));
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.MyProfileResponse.from(profile)));
    }
    @GetMapping("/{memberId}/profile")
    public ResponseEntity<ApiResponse<MemberResponses.PublicProfileResponse>> getPublicProfile(@AuthenticationPrincipal String authenticatedMemberId,
            @PathVariable UUID memberId) {
        parseMemberId(authenticatedMemberId);
        var profile = memberProfileService.getPublicProfile(memberId);
        return ResponseEntity.ok(ApiResponse.ok(MemberResponses.PublicProfileResponse.from(profile)));
    }
    private UUID parseMemberId(String memberId) {
        if (memberId == null || memberId.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try { return UUID.fromString(memberId); }
        catch (IllegalArgumentException exception) { throw new BusinessException(ErrorCode.UNAUTHORIZED); }
    }
}
