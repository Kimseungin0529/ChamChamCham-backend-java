package com.chamchamcham.application.member;

import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class MemberProfileService {
    private static final int DISPLAY_REGION_TOKEN_COUNT = 2;
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final MemberCropRepository memberCropRepository;
    private final CropRepository cropRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MemberProfileService(MemberRepository memberRepository, FarmRepository farmRepository,
            MemberCropRepository memberCropRepository, CropRepository cropRepository,
            UploadedMediaRepository uploadedMediaRepository, ApplicationEventPublisher eventPublisher) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Transactional(readOnly = true)
    public MemberProfileResult.MyProfile getMyProfile(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var member = findMember(memberId);
        var farms = farmRepository.findByOwnerId(memberId);
        var crops = memberCropRepository.findByMemberId(memberId);
        return new MemberProfileResult.MyProfile(required(member.getId(), "Persisted member id is required"),
            member.getEmail(), member.getName(), member.getPhone(), member.getBirthDate(), member.getNickname(),
            member.getExperienceLevel(), member.getManagementType() == null ? null : member.getManagementType().name(),
            member.getProfileMedia() == null ? null : member.getProfileMedia().getFileUrl(),
            farms.stream().map(this::toMyFarm).toList(), toCropProfiles(crops));
    }

    @Transactional(readOnly = true)
    public MemberProfileResult.PublicProfile getPublicProfile(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var member = findMember(memberId);
        var farms = farmRepository.findByOwnerId(memberId);
        var crops = memberCropRepository.findByMemberId(memberId);
        return new MemberProfileResult.PublicProfile(required(member.getId(), "Persisted member id is required"),
            member.getNickname(), member.getExperienceLevel(), member.getManagementType() == null ? null : member.getManagementType().name(),
            member.getProfileMedia() == null ? null : member.getProfileMedia().getFileUrl(),
            farms.stream().map(this::toPublicFarm).toList(), toCropProfiles(crops));
    }

    @Transactional(readOnly = true)
    public List<MemberProfileResult.FarmCrops> getMyFarmCrops(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var farms = farmRepository.findByOwnerId(memberId);
        var cropsByFarmId = new LinkedHashMap<UUID, List<MemberCrop>>();
        for (var link : memberCropRepository.findByMemberId(memberId)) {
            cropsByFarmId.computeIfAbsent(link.getFarm().getId(), id -> new ArrayList<>()).add(link);
        }
        return farms.stream().map(farm -> {
            var farmId = required(farm.getId(), "Persisted farm id is required");
            return new MemberProfileResult.FarmCrops(farmId, farm.getName(), cropsByFarmId.getOrDefault(farmId, List.of())
                .stream().map(link -> CropResult.CropSummary.from(link.getCrop())).toList());
        }).toList();
    }

    public MemberProfileResult.MyProfile updateMyProfile(MemberProfileCommand.UpdateMyProfile command) {
        Objects.requireNonNull(command, "command");
        validateUpdateCommand(command);
        var member = findMember(command.memberId());
        member.completeOnboarding(command.name(), command.phone(), command.birthDate(), command.nickname(),
            command.experienceLevel(), command.managementType());
        syncProfileImage(member, command.profileMediaId());
        for (var farmCommand : command.farms()) {
            var farm = upsertFarm(member, farmCommand);
            syncFarmCrops(member, farm, farmCommand.cropIds());
        }
        return getMyProfile(command.memberId());
    }

    public void withdraw(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var member = memberRepository.findByIdForUpdate(memberId);
        if (member == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        var cloudinaryPublicIds = uploadedMediaRepository.findCloudinaryPublicIdsByOwnerId(memberId).stream().distinct().toList();
        member.updateProfileMedia(null);
        memberRepository.flush();
        memberRepository.hardDeleteById(memberId);
        eventPublisher.publishEvent(new MemberWithdrawalCommitted(memberId, cloudinaryPublicIds));
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateUpdateCommand(MemberProfileCommand.UpdateMyProfile command) {
        if (command.experienceLevel() < 0 || command.experienceLevel() > 100 || command.farms().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        var requestedFarmIds = command.farms().stream().map(MemberProfileCommand.Farm::farmId).filter(Objects::nonNull).toList();
        if (requestedFarmIds.size() != new LinkedHashSet<>(requestedFarmIds).size()) throw new BusinessException(ErrorCode.INVALID_INPUT);
        for (var farm : command.farms()) {
            if (isBlank(farm.name()) || isBlank(farm.roadAddress()) || farm.latitude() == null || farm.longitude() == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (farm.areaSqm() != null && farm.areaSqm().signum() <= 0) throw new BusinessException(ErrorCode.INVALID_INPUT);
            if (farm.cropIds().isEmpty()) throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void syncProfileImage(Member member, @Nullable UUID profileMediaId) {
        var currentMedia = member.getProfileMedia();
        if (profileMediaId == null) {
            if (currentMedia != null) currentMedia.markDeleted();
            member.updateProfileMedia(null);
            return;
        }
        if (currentMedia != null && Objects.equals(currentMedia.getId(), profileMediaId)) return;
        var newMedia = uploadedMediaRepository.findById(profileMediaId).orElseThrow(() -> new BusinessException(ErrorCode.MEDIA_NOT_FOUND));
        validateProfileMedia(member, newMedia);
        if (currentMedia != null) currentMedia.markDeleted();
        newMedia.markAttached();
        member.updateProfileMedia(newMedia);
    }

    private void validateProfileMedia(Member member, UploadedMedia media) {
        if (!Objects.equals(media.getOwner().getId(), member.getId())) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
        if (media.getUsageType() != UploadedMediaUsageType.PROFILE) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
        if (!media.isAttachable()) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
    }

    private Farm upsertFarm(Member member, MemberProfileCommand.Farm command) {
        var boundaryCoordinates = command.boundaryCoordinates().stream()
            .map(c -> new FarmBoundaryCoordinate(c.latitude(), c.longitude())).toList();
        var dataSource = new FarmDataSource(command.dataSource().address(), command.dataSource().coordinate(),
            command.dataSource().parcel(), command.dataSource().landCharacteristic());
        if (command.farmId() == null) {
            return farmRepository.save(new Farm(null, member, command.name(), command.roadAddress(), command.jibunAddress(),
                command.latitude(), command.longitude(), command.pnu(), command.landCategory(), command.areaSqm(),
                command.areaIsManualEntry(), new ArrayList<>(boundaryCoordinates), dataSource));
        }
        var farm = farmRepository.findById(command.farmId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!Objects.equals(farm.getOwner().getId(), member.getId())) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        farm.updateProfile(command.name(), command.roadAddress(), command.jibunAddress(), command.latitude(), command.longitude(),
            command.pnu(), command.landCategory(), command.areaSqm(), command.areaIsManualEntry(), boundaryCoordinates, dataSource);
        return farm;
    }

    private void syncFarmCrops(Member member, Farm farm, List<UUID> cropIds) {
        var distinctCropIds = new ArrayList<>(new LinkedHashSet<>(cropIds));
        var crops = loadCrops(distinctCropIds);
        var farmId = required(farm.getId(), "Persisted farm id is required");
        var memberId = required(member.getId(), "Persisted member id is required");
        memberCropRepository.deleteByMemberIdAndFarmId(memberId, farmId);
        memberCropRepository.saveAll(crops.stream().map(crop -> new MemberCrop(member, farm, crop)).toList());
    }

    private List<Crop> loadCrops(List<UUID> cropIds) {
        var cropsById = new LinkedHashMap<UUID, Crop>();
        cropRepository.findAllById(cropIds).forEach(crop -> cropsById.put(required(crop.getId(), "Persisted crop id is required"), crop));
        if (cropsById.size() != cropIds.size()) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        return cropIds.stream().map(cropsById::get).toList();
    }

    private MemberProfileResult.MyFarm toMyFarm(Farm farm) {
        return new MemberProfileResult.MyFarm(required(farm.getId(), "Persisted farm id is required"),
            farm.getName(), farm.getRoadAddress(), farm.getJibunAddress(), displayRegion(farm));
    }

    private MemberProfileResult.PublicFarm toPublicFarm(Farm farm) {
        return new MemberProfileResult.PublicFarm(required(farm.getId(), "Persisted farm id is required"), displayRegion(farm));
    }

    private List<MemberProfileResult.CropProfile> toCropProfiles(List<MemberCrop> memberCrops) {
        var seen = new LinkedHashSet<UUID>();
        return memberCrops.stream().map(MemberCrop::getCrop)
            .filter(crop -> seen.add(required(crop.getId(), "Persisted crop id is required")))
            .map(crop -> new MemberProfileResult.CropProfile(required(crop.getId(), "Persisted crop id is required"), crop.getName())).toList();
    }

    @Nullable
    private String displayRegion(Farm farm) {
        var address = !isBlank(farm.getRoadAddress()) ? farm.getRoadAddress() : farm.getJibunAddress();
        if (address == null) return null;
        var tokens = Arrays.stream(trim(address).split("\\s+")).filter(token -> !isBlank(token)).limit(DISPLAY_REGION_TOKEN_COUNT).toList();
        return tokens.isEmpty() ? null : String.join(" ", tokens);
    }

    private static boolean isBlank(String value) {
        return value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && (Character.isWhitespace(value.charAt(start)) || Character.isSpaceChar(value.charAt(start)))) start++;
        while (end > start && (Character.isWhitespace(value.charAt(end - 1)) || Character.isSpaceChar(value.charAt(end - 1)))) end--;
        return value.substring(start, end);
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
