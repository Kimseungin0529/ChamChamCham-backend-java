package com.chamchamcham.application.voice;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordResult;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideQueryRepository;
import com.chamchamcham.domain.voice.VoiceRecordSession;
import com.chamchamcham.domain.voice.VoiceRecordSessionRepository;
import com.chamchamcham.domain.voice.VoiceRecordTurnRepository;
import com.chamchamcham.domain.voice.VoiceSessionStatus;
import com.chamchamcham.domain.voice.VoiceTurnRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class VoiceSessionServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private final UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private final UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    @Mock private MemberRepository memberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private MemberCropRepository memberCropRepository;
    @Mock private FarmingRecordRepository farmingRecordRepository;
    @Mock private FarmingRecordService farmingRecordService;
    @Mock private VoiceRecordSessionRepository voiceRecordSessionRepository;
    @Mock private VoiceRecordTurnRepository voiceRecordTurnRepository;
    @Mock private PesticideQueryRepository pesticideQueryRepository;
    private RealtimeSessionProvider realtimeSessionProvider;
    private RealtimeSessionRequest capturedRequest = null;
    private VoiceSessionService service;
    private Member member;
    private Farm farm;
    private Crop crop;

    @BeforeEach
    void setUp() throws Exception {
        realtimeSessionProvider = fakeProvider();
        service = new VoiceSessionService(memberRepository, farmRepository, memberCropRepository, farmingRecordRepository, farmingRecordService,
            voiceRecordSessionRepository, voiceRecordTurnRepository, realtimeSessionProvider, new VoiceSessionProperties(20, 330),
            pesticideQueryRepository);
        member = new Member(memberId, "" + memberId + "@example.com", null);
        farm = new Farm(farmId, member, "약초농장", "서울시 강남구");
        crop = new Crop(cropId, cropId.hashCode(), "황기", CropUsePartCategory.ROOT_BARK);
    }

    @Test
    @DisplayName("세션 생성 시 회원의 농지-작물 정보를 함께 반환한다")
    void 세션_생성_시_회원의_농지_작물_정보를_함께_반환한다() throws Exception {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(java.util.Arrays.asList(farm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(java.util.Arrays.asList(new MemberCrop(member, farm, crop)));
        when(voiceRecordSessionRepository.save(any(VoiceRecordSession.class))).thenAnswer(invocation -> {
            var toSave = invocation.<VoiceRecordSession>getArgument(0);
            return new VoiceRecordSession(sessionId, toSave.getMember(), toSave.getStatus());
        });
        var result = service.create(new VoiceSessionCommand.Create(memberId));
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getClientSecret()).isEqualTo("secret");
        assertThat(result.getFarms()).extracting("farmId").containsExactly(farmId);
        assertThat(result.getCropsByFarm().get(farmId.toString())).extracting("cropId").containsExactly(cropId);
        assertThat(result.getMaxRounds()).isEqualTo(20);
        assertThat(result.getMaxDurationSeconds()).isEqualTo(330);
    }

    @Test
    @DisplayName("OpenAI 토큰 만료는 대화 시간 한도보다 30초 길게 파생된다")
    void OpenAI_토큰_만료는_대화_시간_한도보다_30초_길게_파생된다() throws Exception {
        stubCreateCollaborators();
        service.create(new VoiceSessionCommand.Create(memberId));
        assertThat(capturedRequest.getExpiresAfterSeconds()).isEqualTo(360);
    }

    @Test
    @DisplayName("세션 생성 시 회원 작물에 등록된 농약 목록을 지침에 주입한다")
    void 세션_생성_시_회원_작물에_등록된_농약_목록을_지침에_주입한다() throws Exception {
        stubCreateCollaborators();
        when(pesticideQueryRepository.findByCropNames(java.util.Arrays.asList("황기"),
            200)).thenReturn(java.util.Arrays.asList(new PesticideQueryRepository.VoiceCatalogRow("코니도", "바이엘", "진딧물"),
            new PesticideQueryRepository.VoiceCatalogRow("코니도", "바이엘", "총채벌레")));
        service.create(new VoiceSessionCommand.Create(memberId));
        assertThat(capturedRequest.getInstructions()).contains("코니도(바이엘)");
        assertThat(capturedRequest.getInstructions()).contains("진딧물, 총채벌레");
    }

    @Test
    @DisplayName("작물 매칭 농약이 없고 전체 카탈로그가 작으면 전체 농약명을 주입한다")
    void 작물_매칭_농약이_없고_전체_카탈로그가_작으면_전체_농약명을_주입한다() throws Exception {
        stubCreateCollaborators();
        when(pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(null, null,
            51))).thenReturn(java.util.Arrays.asList(new Pesticide("다이센엠-45", "농협케미컬")));
        service.create(new VoiceSessionCommand.Create(memberId));
        assertThat(capturedRequest.getInstructions()).contains("다이센엠-45(농협케미컬)");
    }

    @Test
    @DisplayName("작물 매칭 농약이 없고 전체 카탈로그가 크면 농약 목록을 생략한다")
    void 작물_매칭_농약이_없고_전체_카탈로그가_크면_농약_목록을_생략한다() throws Exception {
        stubCreateCollaborators();
        when(pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(null, null,
            51))).thenReturn(java.util.stream.IntStream.rangeClosed(1, 51).mapToObj(it -> new Pesticide("농약" + it, "제조사")).toList());
        service.create(new VoiceSessionCommand.Create(memberId));
        assertThat(capturedRequest.getInstructions()).doesNotContain("등록 농약 목록");
    }
    @Test
    void singleCropNamePortArgumentRejectsSetAddAndRemove() {
        stubCreateCollaborators();
        when(pesticideQueryRepository.findByCropNames(anyList(), eq(200))).thenAnswer(invocation -> {
            List<String> names = invocation.getArgument(0);
            assertThat(names).containsExactly("황기");
            assertThrows(UnsupportedOperationException.class, () -> names.set(0, "변경"));
            assertThrows(UnsupportedOperationException.class, () -> names.add("추가"));
            assertThrows(UnsupportedOperationException.class, () -> names.remove(0));
            return List.of();
        });
        service.create(new VoiceSessionCommand.Create(memberId));
        verify(pesticideQueryRepository).findByCropNames(anyList(), eq(200));
    }

    @Test
    void duplicateCropNamesPortArgumentRemainsImmutableSingleton() {
        stubCreateCollaborators();
        var duplicate = new Crop(UUID.fromString("00000000-0000-0000-0000-000000000202"), 423, "황기", CropUsePartCategory.ROOT_BARK);
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(
            new MemberCrop(member, farm, crop), new MemberCrop(member, farm, duplicate)));
        when(pesticideQueryRepository.findByCropNames(anyList(), eq(200))).thenAnswer(invocation -> {
            List<String> names = invocation.getArgument(0);
            assertThat(names).containsExactly("황기");
            assertThrows(UnsupportedOperationException.class, () -> names.set(0, "변경"));
            assertThrows(UnsupportedOperationException.class, () -> names.add("추가"));
            assertThrows(UnsupportedOperationException.class, () -> names.remove(0));
            return List.of();
        });
        service.create(new VoiceSessionCommand.Create(memberId));
        verify(pesticideQueryRepository).findByCropNames(anyList(), eq(200));
    }

    @Test
    void multipleCropNamesPortArgumentKeepsEncounterOrderAndAllowsSetAddAndRemove() {
        stubCreateCollaborators();
        var second = new Crop(UUID.fromString("00000000-0000-0000-0000-000000000202"), 423, "인삼", CropUsePartCategory.ROOT_BARK);
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of(
            new MemberCrop(member, farm, crop), new MemberCrop(member, farm, second), new MemberCrop(member, farm, crop)));
        when(pesticideQueryRepository.findByCropNames(anyList(), eq(200))).thenAnswer(invocation -> {
            List<String> names = invocation.getArgument(0);
            assertThat(names).containsExactly("황기", "인삼");
            assertThat(names.set(0, "변경")).isEqualTo("황기");
            assertThat(names).containsExactly("변경", "인삼");
            assertThat(names.add("추가")).isTrue();
            assertThat(names.remove(1)).isEqualTo("인삼");
            assertThat(names).containsExactly("변경", "추가");
            return List.of();
        });
        service.create(new VoiceSessionCommand.Create(memberId));
        verify(pesticideQueryRepository).findByCropNames(anyList(), eq(200));
    }

    @Test
    void emptyCropNamesDoesNotCallCropFilteredPesticidePort() {
        stubCreateCollaborators();
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(List.of());
        service.create(new VoiceSessionCommand.Create(memberId));
        verify(pesticideQueryRepository, never()).findByCropNames(anyList(), eq(200));
    }

    private void stubCreateCollaborators() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(farmRepository.findByOwnerId(memberId)).thenReturn(java.util.Arrays.asList(farm));
        when(memberCropRepository.findByMemberId(memberId)).thenReturn(java.util.Arrays.asList(new MemberCrop(member, farm, crop)));
        when(voiceRecordSessionRepository.save(any(VoiceRecordSession.class))).thenAnswer(invocation -> {
            var toSave = invocation.<VoiceRecordSession>getArgument(0);
            return new VoiceRecordSession(sessionId, toSave.getMember(), toSave.getStatus());
        });
    }

    @Test
    @DisplayName("대화 턴 제출 시 세션은 확인대기 상태가 되고 누락필드를 계산한다")
    void 대화_턴_제출_시_세션은_확인대기_상태가_되고_누락필드를_계산한다() throws Exception {
        var session = new VoiceRecordSession(sessionId, member, VoiceSessionStatus.CREATED);
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(session);
        var result = service.submitTurns(new VoiceSessionCommand.SubmitTurns(memberId, sessionId,
            java.util.Arrays.asList(new VoiceSessionCommand.TurnInput(VoiceTurnRole.USER, "어제 물 줬어요", null)), new VoiceRecordCandidate(null, null,
            WorkType.WATERING, null, null, null, null, null, null, null, null, null, null)));
        assertThat(result.getStatus()).isEqualTo(VoiceSessionStatus.WAITING_CONFIRMATION);
        assertThat(result.getMissingFields()).containsExactlyInAnyOrder("farmId", "cropId", "workedAt");
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.WAITING_CONFIRMATION);
        assertThat(session.getTranscript()).isEqualTo("USER: 어제 물 줬어요");
    }

    @Test
    @DisplayName("확인대기 상태의 세션을 승인하면 영농일지를 생성하고 세션을 완료 처리한다")
    void 확인대기_상태의_세션을_승인하면_영농일지를_생성하고_세션을_완료_처리한다() throws Exception {
        var session = new VoiceRecordSession(sessionId, member, VoiceSessionStatus.WAITING_CONFIRMATION);
        var record = new FarmingRecord(recordId, member, farm, crop, WorkType.WATERING, LocalDateTime.of(2026, 6, 1, 9, 0), "맑음", 20, "물 줬어요",
            EntryMode.VOICE, false);
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(session);
        when(farmingRecordService.create(createCommand())).thenReturn(new FarmingRecordResult.RecordId(recordId, WorkType.WATERING));
        when(farmingRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        var result = service.confirm(new VoiceSessionCommand.Confirm(memberId, sessionId, createCommand()));
        assertThat(result.getRecordId()).isEqualTo(recordId);
        assertThat(session.getStatus()).isEqualTo(VoiceSessionStatus.COMPLETED);
        assertThat(session.getDraftRecord()).isEqualTo(record);
    }

    @Test
    @DisplayName("생성 상태가 아닌 세션에 대화 턴을 제출하려 하면 예외를 던진다")
    void 생성_상태가_아닌_세션에_대화_턴을_제출하려_하면_예외를_던진다() throws Exception {
        var session = new VoiceRecordSession(sessionId, member, VoiceSessionStatus.WAITING_CONFIRMATION);
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(session);
        var exception = assertThrows(BusinessException.class, () -> {
            service.submitTurns(new VoiceSessionCommand.SubmitTurns(memberId, sessionId,
            java.util.Arrays.asList(new VoiceSessionCommand.TurnInput(VoiceTurnRole.USER, "어제 물 줬어요", null)), new VoiceRecordCandidate(null, null,
            WorkType.WATERING, null, null, null, null, null, null, null, null, null, null)));
        });
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_INVALID_STATE);
    }

    @Test
    @DisplayName("확인대기 상태가 아닌 세션을 승인하려 하면 예외를 던진다")
    void 확인대기_상태가_아닌_세션을_승인하려_하면_예외를_던진다() throws Exception {
        var session = new VoiceRecordSession(sessionId, member, VoiceSessionStatus.CREATED);
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(session);
        var exception = assertThrows(BusinessException.class, () -> service.confirm(new VoiceSessionCommand.Confirm(memberId, sessionId, createCommand())));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_INVALID_STATE);
    }

    @Test
    @DisplayName("완료된 세션은 취소할 수 없다")
    void 완료된_세션은_취소할_수_없다() throws Exception {
        var session = new VoiceRecordSession(sessionId, member, VoiceSessionStatus.COMPLETED);
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(session);
        var exception = assertThrows(BusinessException.class, () -> service.cancel(new VoiceSessionCommand.Cancel(memberId, sessionId)));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_INVALID_STATE);
    }

    @Test
    @DisplayName("존재하지 않거나 남의 세션이면 조회 시 예외를 던진다")
    void 존재하지_않거나_남의_세션이면_조회_시_예외를_던진다() throws Exception {
        when(voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId)).thenReturn(null);
        var exception = assertThrows(BusinessException.class, () -> service.cancel(new VoiceSessionCommand.Cancel(memberId, sessionId)));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VOICE_SESSION_NOT_FOUND);
    }
    private FarmingRecordCommand.Create createCommand() {
        return new FarmingRecordCommand.Create(memberId, farmId, cropId, WorkType.WATERING, LocalDateTime.of(2026, 6, 1, 9, 0), "맑음", 20, "물 줬어요", null,
            null, null, null, null, null, List.of(), EntryMode.VOICE);
    }
    private RealtimeSessionProvider fakeProvider() {
        return request -> {
            capturedRequest = request;
            return new RealtimeSessionResult("secret", LocalDateTime.now(), "gpt-realtime");
        };
    }
}
