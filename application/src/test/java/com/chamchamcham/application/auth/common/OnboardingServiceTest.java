package com.chamchamcham.application.auth.common;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farm.FarmCommand;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class OnboardingServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID cropId = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private final UUID secondCropId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private CropRepository cropRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    private OnboardingStatusResolver onboardingStatusResolver;
    private OnboardingService service;

    @BeforeEach
    void setUp() throws Exception {
        onboardingStatusResolver = new OnboardingStatusResolver();
        service = new OnboardingService(memberRepository, farmRepository, cropRepository, memberCropRepository, uploadedMediaRepository, onboardingStatusResolver);
    }

    @Test
    @DisplayName("complete stores profile farm and crops then returns complete")
    void complete_stores_profile_farm_and_crops_then_returns_complete() throws Exception {
        var member = member();
        var crop = crop(cropId, 422, "참당귀");
        var command = completeOnboardingCommand(java.util.Arrays.asList(cropId), null, "길동", "홍길동");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findAllById(java.util.Arrays.asList(cropId))).thenReturn(java.util.Arrays.asList(crop));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm(member, command.getFarm()));
        var result = service.complete(command);
        assertEquals(memberId, result.getMember().getId());
        assertEquals("홍길동", member.getName());
        assertEquals("010-1234-5678", member.getPhone());
        assertEquals(LocalDate.of(1990, 1, 1), member.getBirthDate());
        assertEquals("길동", member.getNickname());
        assertEquals(72, member.getExperienceLevel());
        assertEquals(ManagementType.AGRICULTURAL_INDIVIDUAL, member.getManagementType());
        assertEquals(farmId, result.getFarm().getId());
        assertEquals("서울 약초농장", result.getFarm().getName());
        assertEquals("서울특별시 강남구 테헤란로 1", result.getFarm().getRoadAddress());
        assertEquals("서울특별시 강남구 역삼동 1", result.getFarm().getJibunAddress());
        assertEquals("4511310200101230004", result.getFarm().getPnu());
        assertThat(result.getCrops().stream().map(it -> it.getId()).toList()).containsExactly(cropId);
        assertEquals(AuthResult.OnboardingStatus.COMPLETE, result.getOnboarding().getStatus());
    }

    @Test
    @DisplayName("complete uses name when nickname is null or blank")
    void complete_uses_name_when_nickname_is_null_or_blank() throws Exception {
        var member = member();
        var crop = crop(cropId, 422, "참당귀");
        var command = completeOnboardingCommand(java.util.Arrays.asList(cropId), null, " ", "홍길동");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findAllById(java.util.Arrays.asList(cropId))).thenReturn(java.util.Arrays.asList(crop));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm(member, command.getFarm()));
        service.complete(command);
        assertEquals("홍길동", member.getNickname());
    }

    @Test
    @DisplayName("complete leaves nickname null when both nickname and name are absent")
    void complete_leaves_nickname_null_when_both_nickname_and_name_are_absent() throws Exception {
        var member = member();
        var crop = crop(cropId, 422, "참당귀");
        var command = completeOnboardingCommand(java.util.Arrays.asList(cropId), null, null, null);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findAllById(java.util.Arrays.asList(cropId))).thenReturn(java.util.Arrays.asList(crop));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm(member, command.getFarm()));
        service.complete(command);
        assertEquals(null, member.getName());
        assertEquals(null, member.getNickname());
    }

    @Test
    @DisplayName("complete rejects missing crop before farm or member crop saves")
    void complete_rejects_missing_crop_before_farm_or_member_crop_saves() throws Exception {
        var member = member();
        var command = completeOnboardingCommand(java.util.Arrays.asList(cropId, secondCropId), null, "길동", "홍길동");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findAllById(java.util.Arrays.asList(cropId, secondCropId))).thenReturn(java.util.Arrays.asList(crop(cropId, 422, "참당귀")));
        var exception = assertThrows(BusinessException.class, () -> service.complete(command));
        assertEquals(ErrorCode.CROP_NOT_FOUND, exception.getErrorCode());
        verify(farmRepository, never()).save(any(Farm.class));
        verifyNoInteractions(memberCropRepository);
    }

    @Test
    @DisplayName("complete rejects missing member")
    void complete_rejects_missing_member() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());
        var exception = assertThrows(BusinessException.class, () -> service.complete(completeOnboardingCommand(java.util.Arrays.asList(cropId), null, "길동", "홍길동")));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(cropRepository, farmRepository, memberCropRepository);
    }

    @Test
    @DisplayName("complete attaches optional profile media")
    void complete_attaches_optional_profile_media() throws Exception {
        var member = member();
        var crop = crop(cropId, 422, "참당귀");
        var media = profileMedia(member);
        var command = completeOnboardingCommand(java.util.Arrays.asList(cropId), Objects.requireNonNull(media.getId()), "길동", "홍길동");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(uploadedMediaRepository.findById(Objects.requireNonNull(media.getId()))).thenReturn(Optional.of(media));
        when(cropRepository.findAllById(java.util.Arrays.asList(cropId))).thenReturn(java.util.Arrays.asList(crop));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm(member, command.getFarm()));
        var result = service.complete(command);
        assertEquals(media, member.getProfileMedia());
        assertEquals(UploadedMediaStatus.ATTACHED, media.getStatus());
        assertEquals("https://example.test/profile.jpg", result.getMember().getProfileImageUrl());
    }

    @Test
    @DisplayName("complete rejects profile media owned by another member")
    void complete_rejects_profile_media_owned_by_another_member() throws Exception {
        var member = member();
        var otherMember = new Member(UUID.fromString("00000000-0000-0000-0000-000000000099"), "other@example.com", null);
        var media = new UploadedMedia(UUID.fromString("00000000-0000-0000-0000-000000000501"), otherMember, UploadedMediaType.IMAGE, UploadedMediaUsageType.PROFILE, "https://example.test/profile.jpg", "profiles/profile");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(uploadedMediaRepository.findById(Objects.requireNonNull(media.getId()))).thenReturn(Optional.of(media));
        var exception = assertThrows(BusinessException.class, () -> service.complete(completeOnboardingCommand(java.util.Arrays.asList(cropId), Objects.requireNonNull(media.getId()), "길동", "홍길동")));
        assertEquals(ErrorCode.MEDIA_NOT_OWNED, exception.getErrorCode());
        verifyNoInteractions(cropRepository, farmRepository, memberCropRepository);
    }
    @Test
    void completePreservesKotlinUnicodeNicknameTrimmingAndCropOrder() {
        var member = member();
        var command = completeOnboardingCommand(List.of(secondCropId, cropId), null, "\u00a0길동\u2007", "홍길동");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(cropRepository.findAllById(command.getCropIds())).thenReturn(List.of(
            crop(cropId, 422, "참당귀"), crop(secondCropId, 423, "더덕")));
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarm(member, command.getFarm()));
        var result = service.complete(command);
        assertEquals("길동", member.getNickname());
        assertThat(result.getCrops()).extracting(x -> x.getId()).containsExactly(secondCropId, cropId);
        assertThat(capturedMemberCrops()).extracting(x -> x.getCrop().getId()).containsExactly(secondCropId, cropId);
    }

    private Member member() {
        return new Member(memberId, "user@example.com", null);
    }
    private Crop crop(UUID id, int externalNo, String name) {
        return new Crop(id, externalNo, name, CropUsePartCategory.ROOT_BARK);
    }
    private AuthCommand.CompleteOnboarding completeOnboardingCommand(List<UUID> cropIds, UUID profileMediaId, String nickname, String name) {
        return new AuthCommand.CompleteOnboarding(memberId, name, "010-1234-5678", LocalDate.of(1990, 1, 1), nickname, 72, ManagementType.AGRICULTURAL_INDIVIDUAL, farmCommand(), cropIds, profileMediaId);
    }
    private FarmCommand.Draft farmCommand() {
        return new FarmCommand.Draft("서울 약초농장", "서울특별시 강남구 테헤란로 1", "서울특별시 강남구 역삼동 1", 35.8465, 127.1292, "4511310200101230004", "전", new BigDecimal("1200.5"), false, java.util.Arrays.asList(new FarmCommand.BoundaryCoordinate(35.8461, 127.1289), new FarmCommand.BoundaryCoordinate(35.8463, 127.1295)), new FarmCommand.DataSource("JUSO", "V_WORLD_ADDRESS", "V_WORLD_CADASTRAL", "V_WORLD_LAND_CHARACTERISTIC"));
    }
    private Farm savedFarm(Member member, FarmCommand.Draft farm) {
        var migratedFarm = new Farm(farmId, member, farm.getName(), farm.getRoadAddress());
        migratedFarm.setJibunAddress(farm.getJibunAddress());
        migratedFarm.setLatitude(farm.getLatitude());
        migratedFarm.setLongitude(farm.getLongitude());
        migratedFarm.setPnu(farm.getPnu());
        migratedFarm.setLandCategory(farm.getLandCategory());
        migratedFarm.setAreaSqm(farm.getAreaSqm());
        migratedFarm.setAreaIsManualEntry(farm.getAreaIsManualEntry());
        return migratedFarm;
    }
    private List<MemberCrop> capturedMemberCrops() {
        var captor = ((ArgumentCaptor<Iterable<MemberCrop>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Iterable.class));
        verify(memberCropRepository).saveAll(captor.capture());
        var values = new ArrayList<MemberCrop>();
        captor.getValue().forEach(values::add);
        return values;
    }
    private UploadedMedia profileMedia(Member member) {
        return new UploadedMedia(UUID.fromString("00000000-0000-0000-0000-000000000501"), member, UploadedMediaType.IMAGE, UploadedMediaUsageType.PROFILE, "https://example.test/profile.jpg", "profiles/profile");
    }
}
