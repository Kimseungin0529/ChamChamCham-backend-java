package com.chamchamcham.application.auth.common;

import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Service
@Transactional
public class OnboardingService {
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final MemberCropRepository memberCropRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final OnboardingStatusResolver onboardingStatusResolver;

    public OnboardingService(MemberRepository memberRepository, FarmRepository farmRepository, CropRepository cropRepository, MemberCropRepository memberCropRepository, UploadedMediaRepository uploadedMediaRepository, OnboardingStatusResolver onboardingStatusResolver) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.onboardingStatusResolver = Objects.requireNonNull(onboardingStatusResolver, "onboardingStatusResolver");
    }
    public AuthResult.OnboardingComplete complete(AuthCommand.CompleteOnboarding command) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findById(command.memberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        com.chamchamcham.domain.media.UploadedMedia profileMedia = null;
        if (command.profileMediaId() != null) {
            profileMedia = uploadedMediaRepository.findById(command.profileMediaId()).orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
            if (!Objects.equals(profileMedia.getOwner().getId(), member.getId())) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
            if (profileMedia.getUsageType() != UploadedMediaUsageType.PROFILE) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
            if (!profileMedia.isAttachable()) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
        }
        var requestedCropIds = command.cropIds();
        var cropsById = new LinkedHashMap<java.util.UUID, com.chamchamcham.domain.crop.Crop>();
        for (var crop : cropRepository.findAllById(requestedCropIds)) {
            if (crop.getId() == null) throw new IllegalArgumentException("Persisted crop id is required");
            cropsById.put(crop.getId(), crop);
        }
        if (cropsById.size() != requestedCropIds.size()) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        var crops = new ArrayList<com.chamchamcham.domain.crop.Crop>();
        for (var cropId : requestedCropIds) {
            if (!cropsById.containsKey(cropId)) throw new java.util.NoSuchElementException("Key " + cropId + " is missing in the map.");
            crops.add(cropsById.get(cropId));
        }
        var nickname = trimmedOrNull(command.nickname());
        member.completeOnboarding(command.name(), command.phone(), command.birthDate(), nickname != null ? nickname : trimmedOrNull(command.name()), command.experienceLevel(), command.managementType());
        if (profileMedia != null) {
            member.updateProfileMedia(profileMedia);
            profileMedia.markAttached();
        }
        var farm = farmRepository.save(command.farm().toFarm(member));
        var memberCrops = new ArrayList<MemberCrop>();
        for (var crop : crops) memberCrops.add(new MemberCrop(member, farm, crop));
        memberCropRepository.saveAll(memberCrops);
        var summaries = new ArrayList<CropResult.CropSummary>();
        for (var crop : crops) summaries.add(CropResult.CropSummary.from(crop));
        return new AuthResult.OnboardingComplete(AuthResult.MemberProfile.from(member), AuthResult.FarmSummary.from(farm), summaries, onboardingStatusResolver.resolve(member));
    }
    private static String trimmedOrNull(String value) {
        if (value == null) return null;
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return start == end ? null : value.substring(start, end);
    }
    private static boolean whitespace(char c) { return Character.isWhitespace(c) || Character.isSpaceChar(c); }

}
