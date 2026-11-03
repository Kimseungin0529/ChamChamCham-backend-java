package com.chamchamcham.application.farm;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID oldCropId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID newCropId = UUID.fromString("00000000-0000-0000-0000-000000000203");
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private CropRepository cropRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    private FarmService service;
    private Member member;
    private Farm farm;
    private Crop crop;
    private Crop oldCrop;
    private Crop newCrop;

    @BeforeEach
    void setUp() {
        member = new Member(memberId, "member@example.com", null);
        farm = new Farm(farmId, member, "기존 농장", "강원특별자치도 횡성군 둔내면 1");
        farm.setLatitude(37.5); farm.setLongitude(128.1);
        crop = crop(cropId, "황기"); oldCrop = crop(oldCropId, "당귀"); newCrop = crop(newCropId, "작약");
        service = new FarmService(memberRepository, farmRepository, cropRepository, memberCropRepository, farmingRecordRepository);
    }

    @Test
    void createSavesOwnedFarmAndRequestedCropLinks() {
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(cropRepository.findAllById(List.of(cropId))).willReturn(List.of(crop));
        given(farmRepository.save(any(Farm.class))).willReturn(farm);
        var result = service.create(createCommand(List.of(cropId)));
        assertEquals(farmId, result.getFarmId());
        assertThat(result.getCrops()).extracting(c -> c.getId()).containsExactly(cropId);
        assertThat(capturedSavedMemberCrops()).extracting(c -> c.getCrop().getId()).containsExactly(cropId);
    }

    @Test
    void createRejectsMissingCrop() {
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(cropRepository.findAllById(List.of(cropId))).willReturn(List.of());
        var exception = assertThrows(BusinessException.class, () -> service.create(createCommand(List.of(cropId))));
        assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    void replaceRemovesUnusedCropLinkAndAddsRequestedCropLink() {
        var oldLink = new MemberCrop(member, farm, oldCrop);
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(cropRepository.findAllById(List.of(newCropId))).willReturn(List.of(newCrop));
        given(memberCropRepository.findAllWithCropByMemberIdAndFarmId(memberId, farmId)).willReturn(List.of(oldLink));
        given(farmingRecordRepository.existsByMember_IdAndFarm_IdAndCrop_Id(memberId, farmId, oldCropId)).willReturn(false);
        var result = service.replace(replaceCommand(List.of(newCropId)));
        assertEquals("수정 농장", farm.getName());
        assertThat(result.getCrops()).extracting(c -> c.getId()).containsExactly(newCropId);
        verify(memberCropRepository).deleteAll(List.of(oldLink));
        assertThat(capturedSavedMemberCrops()).extracting(c -> c.getCrop().getId()).containsExactly(newCropId);
    }

    @Test
    void replaceRejectsRemovalOfCropWithFarmingRecord() {
        var oldLink = new MemberCrop(member, farm, oldCrop);
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(cropRepository.findAllById(List.of(newCropId))).willReturn(List.of(newCrop));
        given(memberCropRepository.findAllWithCropByMemberIdAndFarmId(memberId, farmId)).willReturn(List.of(oldLink));
        given(farmingRecordRepository.existsByMember_IdAndFarm_IdAndCrop_Id(memberId, farmId, oldCropId)).willReturn(true);
        var exception = assertThrows(BusinessException.class, () -> service.replace(replaceCommand(List.of(newCropId))));
        assertEquals(ErrorCode.FARM_CROP_IN_USE, exception.getErrorCode());
        verify(memberCropRepository, never()).deleteAll(any());
        assertEquals("기존 농장", farm.getName());
    }

    @Test
    void deleteRejectsFarmReferencedByFarmingRecord() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(farmingRecordRepository.existsByFarm_Id(farmId)).willReturn(true);
        var exception = assertThrows(BusinessException.class, () -> service.delete(new FarmCommand.Delete(memberId, farmId)));
        assertEquals(ErrorCode.FARM_IN_USE, exception.getErrorCode());
        verify(farmRepository, never()).delete(farm);
    }

    @Test
    void deleteRemovesCropLinksBeforeDeletingUnreferencedFarm() {
        given(farmRepository.findByIdAndOwnerId(farmId, memberId)).willReturn(farm);
        given(farmingRecordRepository.existsByFarm_Id(farmId)).willReturn(false);
        service.delete(new FarmCommand.Delete(memberId, farmId));
        var order = inOrder(memberCropRepository, farmRepository);
        order.verify(memberCropRepository).deleteByMemberIdAndFarmId(memberId, farmId);
        order.verify(farmRepository).delete(farm);
    }

    private FarmCommand.Create createCommand(List<UUID> cropIds) {
        return new FarmCommand.Create(memberId, draft("새 농장"), cropIds);
    }
    private FarmCommand.Replace replaceCommand(List<UUID> cropIds) {
        return new FarmCommand.Replace(memberId, farmId, draft("수정 농장"), cropIds);
    }
    private FarmCommand.Draft draft(String name) {
        return new FarmCommand.Draft(name, "강원특별자치도 횡성군 둔내면 2", null, 37.5, 128.1,
            "4273031021101010000", "전", new BigDecimal("1200.5"), false,
            List.of(new FarmCommand.BoundaryCoordinate(37.5, 128.1)), new FarmCommand.DataSource("JUSO", "VWORLD", "VWORLD", "VWORLD"));
    }
    private Crop crop(UUID id, String name) { return new Crop(id, id.hashCode(), name, CropUsePartCategory.ROOT_BARK); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<MemberCrop> capturedSavedMemberCrops() {
        ArgumentCaptor<Iterable<MemberCrop>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(memberCropRepository).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }
}
