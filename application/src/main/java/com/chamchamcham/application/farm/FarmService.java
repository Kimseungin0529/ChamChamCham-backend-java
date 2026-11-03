package com.chamchamcham.application.farm;

import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class FarmService {
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final MemberCropRepository memberCropRepository;
    private final FarmingRecordRepository farmingRecordRepository;

    public FarmService(MemberRepository memberRepository, FarmRepository farmRepository, CropRepository cropRepository,
                       MemberCropRepository memberCropRepository, FarmingRecordRepository farmingRecordRepository) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
    }

    @Transactional(readOnly = true)
    public List<FarmResult.Detail> list(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var cropsByFarmId = new LinkedHashMap<UUID, List<MemberCrop>>();
        for (var link : memberCropRepository.findAllWithCropByMemberId(memberId)) {
            cropsByFarmId.computeIfAbsent(required(link.getFarm().getId(), "Persisted farm id is required"), id -> new ArrayList<>()).add(link);
        }
        return farmRepository.findAllWithBoundaryCoordinatesByOwnerId(memberId).stream().map(farm -> {
            var crops = cropsByFarmId.getOrDefault(required(farm.getId(), "Persisted farm id is required"), List.of())
                .stream().map(link -> CropResult.CropSummary.from(link.getCrop())).toList();
            return FarmResult.Detail.from(farm, crops);
        }).toList();
    }

    public FarmResult.Detail create(FarmCommand.Create command) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findById(command.memberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var crops = loadCrops(command.cropIds());
        var farm = farmRepository.save(command.draft().toFarm(member));
        memberCropRepository.saveAll(crops.stream().map(crop -> new MemberCrop(member, farm, crop)).toList());
        return FarmResult.Detail.from(farm, crops.stream().map(CropResult.CropSummary::from).toList());
    }

    public FarmResult.Detail replace(FarmCommand.Replace command) {
        Objects.requireNonNull(command, "command");
        var farm = findOwnedFarm(command.memberId(), command.farmId());
        var requestedCrops = loadCrops(command.cropIds());
        var currentLinks = memberCropRepository.findAllWithCropByMemberIdAndFarmId(command.memberId(), command.farmId());
        var requestedCropIds = new LinkedHashSet<UUID>();
        requestedCrops.forEach(crop -> requestedCropIds.add(required(crop.getId(), "Persisted crop id is required")));
        var removedLinks = currentLinks.stream().filter(link -> !requestedCropIds.contains(required(link.getCrop().getId(), "Required value was null."))).toList();
        assertRemovable(command.memberId(), command.farmId(), removedLinks);
        command.draft().applyTo(farm);
        if (!removedLinks.isEmpty()) memberCropRepository.deleteAll(removedLinks);
        var currentCropIds = new LinkedHashSet<UUID>();
        currentLinks.forEach(link -> currentCropIds.add(required(link.getCrop().getId(), "Required value was null.")));
        var addedLinks = requestedCrops.stream().filter(crop -> !currentCropIds.contains(required(crop.getId(), "Required value was null.")))
            .map(crop -> new MemberCrop(farm.getOwner(), farm, crop)).toList();
        if (!addedLinks.isEmpty()) memberCropRepository.saveAll(addedLinks);
        return FarmResult.Detail.from(farm, requestedCrops.stream().map(CropResult.CropSummary::from).toList());
    }

    public void delete(FarmCommand.Delete command) {
        Objects.requireNonNull(command, "command");
        var farm = findOwnedFarm(command.memberId(), command.farmId());
        if (farmingRecordRepository.existsByFarm_Id(command.farmId())) throw new BusinessException(ErrorCode.FARM_IN_USE);
        memberCropRepository.deleteByMemberIdAndFarmId(command.memberId(), command.farmId());
        farmRepository.delete(farm);
    }

    private Farm findOwnedFarm(UUID memberId, UUID farmId) {
        var farm = farmRepository.findByIdAndOwnerId(farmId, memberId);
        if (farm == null) throw new BusinessException(ErrorCode.FARM_NOT_FOUND);
        return farm;
    }

    private List<Crop> loadCrops(List<UUID> cropIds) {
        var cropsById = new LinkedHashMap<UUID, Crop>();
        cropRepository.findAllById(cropIds).forEach(crop -> cropsById.put(required(crop.getId(), "Persisted crop id is required"), crop));
        if (cropsById.size() != cropIds.size()) throw new BusinessException(ErrorCode.CROP_NOT_FOUND);
        return cropIds.stream().map(cropsById::get).toList();
    }

    private void assertRemovable(UUID memberId, UUID farmId, List<MemberCrop> links) {
        for (var link : links) {
            if (farmingRecordRepository.existsByMember_IdAndFarm_IdAndCrop_Id(memberId, farmId, required(link.getCrop().getId(), "Required value was null."))) {
                throw new BusinessException(ErrorCode.FARM_CROP_IN_USE);
            }
        }
    }

    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
