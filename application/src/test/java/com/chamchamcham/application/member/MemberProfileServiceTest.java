package com.chamchamcham.application.member;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.member.ManagementType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID secondFarmId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID secondCropId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private CropRepository cropRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    private MemberProfileService service;
    private Member member;
    private Farm farm;
    private Crop crop;
    private Crop secondCrop;

    @BeforeEach
    void setUp() {
        member = member();
        member.updateProfileMedia(profileMedia(member));
        farm = farm(farmId, "강원특별자치도 횡성군 둔내면 샘물로 12", "강원특별자치도 횡성군 둔내면 현천리 101");
        crop = crop(cropId, "황기"); secondCrop = crop(secondCropId, "인삼");
        service = new MemberProfileService(memberRepository, farmRepository, memberCropRepository,
            cropRepository, uploadedMediaRepository, eventPublisher);
    }

    @Test
    void getMyProfileReturnsPrivateMemberFieldsFarmsAndCrops() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(
            List.of(memberCrop(farm, crop), memberCrop(farm, crop), memberCrop(farm, secondCrop))
        );

        var profile = service.getMyProfile(memberId);

        assertEquals(memberId, profile.getMemberId());
        assertEquals("hwanggi@example.com", profile.getEmail());
        assertEquals("이황기", profile.getName());
        assertEquals("010-1000-0001", profile.getPhone());
        assertEquals(LocalDate.of(1986, 3, 12), profile.getBirthDate());
        assertEquals("황기농부", profile.getNickname());
        assertEquals(2, profile.getExperienceLevel());
        assertEquals("AGRICULTURAL_INDIVIDUAL", profile.getManagementType());
        assertEquals("https://example.test/" + mediaId + ".jpg", profile.getProfileImageUrl());
        assertEquals("강원특별자치도 횡성군", profile.getFarms().getFirst().getDisplayRegion());
        assertEquals("강원특별자치도 횡성군 둔내면 샘물로 12", profile.getFarms().getFirst().getRoadAddress());
        assertThat(profile.getCrops().stream().map(it -> it.getCropId()).toList()).containsExactly(cropId, secondCropId);
    }

    @Test
    void getMyFarmCropsGroupsCropsByFarm() {
        var secondFarm = farm(secondFarmId, "제주특별자치도 서귀포시 색달동 1", "강원특별자치도 횡성군 둔내면 현천리 101");
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm, secondFarm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(
            List.of(memberCrop(farm, crop), memberCrop(farm, secondCrop))
        );

        var farmCrops = service.getMyFarmCrops(memberId);

        assertEquals(2, farmCrops.size());
        var firstFarmCrops = farmCrops.stream().filter(it -> Objects.equals(it.getFarmId(), farmId)).findFirst().orElseThrow();
        assertThat(firstFarmCrops.getCrops().stream().map(it -> it.getId()).toList()).containsExactly(cropId, secondCropId);
        var secondFarmCrops = farmCrops.stream().filter(it -> Objects.equals(it.getFarmId(), secondFarmId)).findFirst().orElseThrow();
        assertThat(secondFarmCrops.getCrops()).isEmpty();
    }

    @Test
    void getPublicProfileExcludesPrivateFieldsAndReturnsRegionAndCrops() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(memberCrop(farm, crop)));

        var profile = service.getPublicProfile(memberId);

        assertEquals(memberId, profile.getMemberId());
        assertEquals("황기농부", profile.getNickname());
        assertEquals(2, profile.getExperienceLevel());
        assertEquals("AGRICULTURAL_INDIVIDUAL", profile.getManagementType());
        assertEquals("https://example.test/" + mediaId + ".jpg", profile.getProfileImageUrl());
        assertEquals("강원특별자치도 횡성군", profile.getFarms().getFirst().getDisplayRegion());
        assertThat(profile.getCrops().stream().map(it -> it.getCropName()).toList()).containsExactly("황기");
    }

    @Test
    void displayRegionUsesFirstTwoTokensAndFallsBackToJibunAddress() {
        var blankRoadFarm = farm(secondFarmId, " ", "제주특별자치도 서귀포시 색달동 1");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(blankRoadFarm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of());

        var profile = service.getPublicProfile(memberId);

        assertEquals("제주특별자치도 서귀포시", profile.getFarms().getFirst().getDisplayRegion());
    }

    @Test
    void getProfileRejectsMissingMember() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        var exception = assertThrows(BusinessException.class, () -> service.getPublicProfile(memberId));

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(farmRepository);
        verifyNoInteractions(memberCropRepository);
    }

    @Test
    void updateMyProfileChangesBasicFieldsKeepsCurrentImageUpdatesFarmAndReplacesFarmCrops() {
        var currentProfileMedia = Objects.requireNonNull(member.getProfileMedia());
        var command = updateCommand(currentProfileMedia.getId(), List.of(updateFarmCommand(farmId, List.of(cropId, secondCropId))));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));
        when(cropRepository.findAllById(List.of(cropId, secondCropId))).thenReturn(List.of(crop, secondCrop));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(
            List.of(memberCrop(farm, crop), memberCrop(farm, secondCrop))
        );
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));

        var profile = service.updateMyProfile(command);

        assertEquals("수정 이름", member.getName());
        assertEquals("010-2000-0002", member.getPhone());
        assertEquals(LocalDate.of(1990, 4, 5), member.getBirthDate());
        assertEquals("수정닉", member.getNickname());
        assertEquals(7, member.getExperienceLevel());
        assertEquals(ManagementType.NON_REGISTERED_FARMER, member.getManagementType());
        assertEquals(UploadedMediaStatus.ATTACHED, currentProfileMedia.getStatus());
        assertEquals("수정 농장", farm.getName());
        assertEquals("강원특별자치도 평창군 새로 2", farm.getRoadAddress());
        verify(memberCropRepository).deleteByMemberIdAndFarmId(memberId, farmId);
        assertThat(capturedMemberCrops().stream().map(it -> it.getCrop().getId()).toList()).containsExactly(cropId, secondCropId);
        assertEquals(memberId, profile.getMemberId());
    }

    @Test
    void updateMyProfileSucceedsWhenOptionalIdentityFieldsAreAbsent() {
        var command = updateCommand(Objects.requireNonNull(member.getProfileMedia()).getId(), List.of(updateFarmCommand(farmId, List.of(cropId))));
        command = new MemberProfileCommand.UpdateMyProfile(command.getMemberId(), null, null, null, null, command.getExperienceLevel(), command.getManagementType(), command.getProfileMediaId(), command.getFarms());
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));
        when(cropRepository.findAllById(List.of(cropId))).thenReturn(List.of(crop));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(memberCrop(farm, crop)));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));

        service.updateMyProfile(command);

        assertNull(member.getName());
        assertNull(member.getPhone());
        assertNull(member.getBirthDate());
        assertNull(member.getNickname());
        assertEquals(7, member.getExperienceLevel());
    }

    @Test
    void updateMyProfileReplacesProfileImageAndDeletesPreviousImage() {
        var previousMedia = Objects.requireNonNull(member.getProfileMedia());
        var newMediaId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var newMedia = profileMedia(member, newMediaId, UploadedMediaUsageType.PROFILE, UploadedMediaStatus.TEMP);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(uploadedMediaRepository.findById(newMediaId)).thenReturn(Optional.of(newMedia));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));
        when(cropRepository.findAllById(List.of(cropId))).thenReturn(List.of(crop));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(memberCrop(farm, crop)));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));

        service.updateMyProfile(updateCommand(newMediaId, List.of(updateFarmCommand(farmId, List.of(cropId)))));

        assertEquals(UploadedMediaStatus.DELETED, previousMedia.getStatus());
        assertEquals(UploadedMediaStatus.ATTACHED, newMedia.getStatus());
        assertEquals(newMedia, member.getProfileMedia());
    }

    @Test
    void updateMyProfileRemovesProfileImageWhenProfileMediaIdIsNull() {
        var previousMedia = Objects.requireNonNull(member.getProfileMedia());
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));
        when(cropRepository.findAllById(List.of(cropId))).thenReturn(List.of(crop));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(memberCrop(farm, crop)));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm));

        service.updateMyProfile(updateCommand(null, List.of(updateFarmCommand(farmId, List.of(cropId)))));

        assertEquals(UploadedMediaStatus.DELETED, previousMedia.getStatus());
        assertEquals(null, member.getProfileMedia());
    }

    @Test
    void updateMyProfileAddsNewFarmAndKeepsOmittedExistingFarms() {
        var newFarmId = UUID.fromString("00000000-0000-0000-0000-000000000103");
        var savedFarm = farm(newFarmId, "강원특별자치도 평창군 새로 2", "강원특별자치도 횡성군 둔내면 현천리 101");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm);
        when(cropRepository.findAllById(List.of(secondCropId))).thenReturn(List.of(secondCrop));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(memberCrop(savedFarm, secondCrop)));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(farm, savedFarm));

        service.updateMyProfile(
            updateCommand(Objects.requireNonNull(member.getProfileMedia()).getId(), List.of(updateFarmCommand(null, List.of(secondCropId))))
        );

        verify(farmRepository, never()).delete(any(Farm.class));
        verify(memberCropRepository).deleteByMemberIdAndFarmId(memberId, newFarmId);
        assertThat(capturedMemberCrops().stream().map(it -> it.getCrop().getId()).toList()).containsExactly(secondCropId);
    }

    @Test
    void updateMyProfileRejectsAnotherMemberFarm() {
        var otherMember = new Member(otherMemberId, "other@example.com", null);
        var otherFarm = new Farm(farmId, otherMember, "남의 농장", "서울특별시 강남구");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(otherFarm));

        var exception = assertThrows(BusinessException.class, () -> service.updateMyProfile(
                updateCommand(Objects.requireNonNull(member.getProfileMedia()).getId(), List.of(updateFarmCommand(farmId, List.of(cropId))))
            ));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(cropRepository);
    }

    @Test
    void updateMyProfileRejectsMissingCrop() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findById(farmId)).thenReturn(Optional.of(farm));
        when(cropRepository.findAllById(List.of(cropId))).thenReturn(List.of());

        var exception = assertThrows(BusinessException.class, () -> service.updateMyProfile(
                updateCommand(Objects.requireNonNull(member.getProfileMedia()).getId(), List.of(updateFarmCommand(farmId, List.of(cropId))))
            ));

        assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMyProfileRejectsAnotherMemberProfileImage() {
        var otherMediaId = UUID.fromString("00000000-0000-0000-0000-000000000302");
        var otherMember = new Member(otherMemberId, "other@example.com", null);
        var otherMedia = profileMedia(otherMember, otherMediaId, UploadedMediaUsageType.PROFILE, UploadedMediaStatus.TEMP);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(uploadedMediaRepository.findById(otherMediaId)).thenReturn(Optional.of(otherMedia));

        var exception = assertThrows(BusinessException.class, () -> service.updateMyProfile(
                updateCommand(otherMediaId, List.of(updateFarmCommand(farmId, List.of(cropId))))
            ));

        assertEquals(ErrorCode.MEDIA_NOT_OWNED, exception.getErrorCode());
    }

    @Test
    void withdrawClearsProfileBulkDeletesMemberAndPublishesDistinctCloudinaryIds() {
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(member);
        when(uploadedMediaRepository.findCloudinaryPublicIdsByOwnerId(memberId))
            .thenReturn(List.of("profile/image", "community/image", "community/image"));

        service.withdraw(memberId);

        assertNull(member.getProfileMedia());
        verify(memberRepository).flush();
        verify(memberRepository).hardDeleteById(memberId);
        var event = capturedWithdrawalEvent();
        assertEquals(memberId, event.getMemberId());
        assertThat(event.getCloudinaryPublicIds())
            .containsExactlyInAnyOrder("profile/image", "community/image");
    }

    @Test
    void withdrawRejectsMissingMemberBeforeMediaLookup() {
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(null);

        var exception = assertThrows(BusinessException.class, () -> service.withdraw(memberId));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(uploadedMediaRepository, eventPublisher);
    }

    @Test
    void displayRegionTrimsUnicodeSpaceAroundFallbackAddress() {
        var unicodeFarm = farm(secondFarmId, "\u00a0", "\u00a0제주특별자치도 서귀포시 색달동 1\u00a0");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(List.of(unicodeFarm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of());
        assertEquals("제주특별자치도 서귀포시", service.getPublicProfile(memberId).getFarms().getFirst().getDisplayRegion());
    }

    @Test
    void updateRejectsUnicodeBlankFarmNameBeforeLoadingMember() {
        var base = updateFarmCommand(farmId, List.of(cropId));
        var invalidFarm = new MemberProfileCommand.Farm(base.getFarmId(), "\u00a0", base.getRoadAddress(),
            base.getJibunAddress(), base.getLatitude(), base.getLongitude(), base.getPnu(), base.getLandCategory(),
            base.getAreaSqm(), base.getAreaIsManualEntry(), base.getBoundaryCoordinates(), base.getDataSource(), base.getCropIds());
        var exception = assertThrows(BusinessException.class,
            () -> service.updateMyProfile(updateCommand(null, List.of(invalidFarm))));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(memberRepository, farmRepository, memberCropRepository, cropRepository, uploadedMediaRepository);
    }

    private Member member() {
        var value = new Member(memberId, "hwanggi@example.com", null);
        value.setName("이황기"); value.setPhone("010-1000-0001"); value.setBirthDate(LocalDate.of(1986, 3, 12));
        value.setNickname("황기농부"); value.setExperienceLevel(2); value.setManagementType(ManagementType.AGRICULTURAL_INDIVIDUAL);
        return value;
    }
    private UploadedMedia profileMedia(Member owner) {
        return profileMedia(owner, mediaId, UploadedMediaUsageType.PROFILE, UploadedMediaStatus.ATTACHED);
    }
    private UploadedMedia profileMedia(Member owner, UUID id, UploadedMediaUsageType usageType, UploadedMediaStatus status) {
        var value = new UploadedMedia(id, owner, UploadedMediaType.IMAGE, usageType, "https://example.test/" + id + ".jpg", "profile/" + id);
        value.setStatus(status); return value;
    }
    private Farm farm(UUID id, String roadAddress, String jibunAddress) {
        var value = new Farm(id, member, "횡성 황기밭", roadAddress);
        value.setJibunAddress(jibunAddress); return value;
    }
    private Crop crop(UUID id, String name) { return new Crop(id, id.hashCode(), name, CropUsePartCategory.ROOT_BARK); }
    private MemberCrop memberCrop(Farm farm, Crop crop) { return new MemberCrop(member, farm, crop); }
    private MemberProfileCommand.UpdateMyProfile updateCommand(UUID profileMediaId, List<MemberProfileCommand.Farm> farms) {
        return new MemberProfileCommand.UpdateMyProfile(memberId, "수정 이름", "010-2000-0002", LocalDate.of(1990, 4, 5),
            "수정닉", 7, ManagementType.NON_REGISTERED_FARMER, profileMediaId, farms);
    }
    private MemberProfileCommand.Farm updateFarmCommand(UUID farmId, List<UUID> cropIds) {
        return new MemberProfileCommand.Farm(farmId, "수정 농장", "강원특별자치도 평창군 새로 2", null, 37.2, 128.2,
            "new-pnu", "답", new BigDecimal("200.50"), true,
            List.of(new MemberProfileCommand.FarmBoundaryCoordinate(37.2, 128.2)),
            new MemberProfileCommand.FarmDataSource("KAKAO", "KAKAO", "PUBLIC_DATA", null), cropIds);
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<MemberCrop> capturedMemberCrops() {
        ArgumentCaptor<Iterable<MemberCrop>> captor = (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
        verify(memberCropRepository).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }
    private MemberWithdrawalCommitted capturedWithdrawalEvent() {
        var captor = ArgumentCaptor.forClass(MemberWithdrawalCommitted.class);
        verify(eventPublisher).publishEvent(captor.capture()); return captor.getValue();
    }
}
