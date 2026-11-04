package com.chamchamcham.application.voice;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farming.FarmingRecordCommand;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.domain.crop.MemberCropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import com.chamchamcham.domain.pesticide.PesticideQueryRepository;
import com.chamchamcham.domain.voice.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class VoiceSessionService {
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final MemberCropRepository memberCropRepository;
    private final FarmingRecordRepository farmingRecordRepository;
    private final FarmingRecordService farmingRecordService;
    private final VoiceRecordSessionRepository voiceRecordSessionRepository;
    private final VoiceRecordTurnRepository voiceRecordTurnRepository;
    private final RealtimeSessionProvider realtimeSessionProvider;
    private final VoiceSessionProperties voiceSessionProperties;
    private final PesticideQueryRepository pesticideQueryRepository;
    private static final int MAX_PESTICIDE_CATALOG_ROWS = 200;
    private static final int MAX_PESTICIDE_OPTIONS = 50;
    private static final int MAX_PESTS_PER_PESTICIDE = 5;
    private static final int EXPIRY_BUFFER_SECONDS = 30;

    public VoiceSessionService(MemberRepository memberRepository, FarmRepository farmRepository,
                               MemberCropRepository memberCropRepository, FarmingRecordRepository farmingRecordRepository,
                               FarmingRecordService farmingRecordService, VoiceRecordSessionRepository voiceRecordSessionRepository,
                               VoiceRecordTurnRepository voiceRecordTurnRepository, RealtimeSessionProvider realtimeSessionProvider,
                               VoiceSessionProperties voiceSessionProperties, PesticideQueryRepository pesticideQueryRepository) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
        this.farmingRecordService = Objects.requireNonNull(farmingRecordService, "farmingRecordService");
        this.voiceRecordSessionRepository = Objects.requireNonNull(voiceRecordSessionRepository, "voiceRecordSessionRepository");
        this.voiceRecordTurnRepository = Objects.requireNonNull(voiceRecordTurnRepository, "voiceRecordTurnRepository");
        this.realtimeSessionProvider = Objects.requireNonNull(realtimeSessionProvider, "realtimeSessionProvider");
        this.voiceSessionProperties = Objects.requireNonNull(voiceSessionProperties, "voiceSessionProperties");
        this.pesticideQueryRepository = Objects.requireNonNull(pesticideQueryRepository, "pesticideQueryRepository");
    }

    public VoiceSessionResult.Created create(VoiceSessionCommand.Create command) {
        Objects.requireNonNull(command, "command");
        var member = findMember(command.getMemberId());
        var farms = new ArrayList<FarmOption>();
        for (var farm : farmRepository.findByOwnerId(command.getMemberId())) farms.add(new FarmOption(required(farm.getId()), farm.getName()));
        var groupedCrops = new LinkedHashMap<String, List<com.chamchamcham.domain.crop.MemberCrop>>();
        for (var memberCrop : memberCropRepository.findByMemberId(command.getMemberId())) {
            groupedCrops.computeIfAbsent(String.valueOf(memberCrop.getFarm().getId()), key -> new ArrayList<>()).add(memberCrop);
        }
        var cropsByFarm = new LinkedHashMap<String, List<CropOption>>();
        for (var entry : groupedCrops.entrySet()) {
            var seen = new LinkedHashSet<UUID>();
            var crops = new ArrayList<CropOption>();
            for (var memberCrop : entry.getValue()) {
                var option = new CropOption(required(memberCrop.getCrop().getId()), memberCrop.getCrop().getName());
                if (seen.add(option.getCropId())) crops.add(option);
            }
            cropsByFarm.put(entry.getKey(), crops);
        }
        var tool = FarmingRecordVoiceToolSchema.build(farms, cropsByFarm);
        var instructions = VoiceSessionInstructions.build(farms, cropsByFarm, loadPesticideOptions(cropsByFarm), LocalDateTime.now(),
            voiceSessionProperties.getMaxRounds(), voiceSessionProperties.getMaxDurationSeconds());
        var provider = realtimeSessionProvider.createEphemeralSession(new RealtimeSessionRequest(instructions, Collections.singletonList(tool),
            voiceSessionProperties.getMaxDurationSeconds() + EXPIRY_BUFFER_SECONDS));
        var session = voiceRecordSessionRepository.save(new VoiceRecordSession(member, VoiceSessionStatus.CREATED));
        return new VoiceSessionResult.Created(required(session.getId()), provider.getClientSecret(), provider.getExpiresAt(), provider.getModel(),
            farms, cropsByFarm, voiceSessionProperties.getMaxRounds(), voiceSessionProperties.getMaxDurationSeconds());
    }

    public VoiceSessionResult.Processed submitTurns(VoiceSessionCommand.SubmitTurns command) {
        Objects.requireNonNull(command, "command");
        var session = findSession(command.getSessionId(), command.getMemberId());
        if (session.getStatus() != VoiceSessionStatus.CREATED) throw new BusinessException(ErrorCode.VOICE_SESSION_INVALID_STATE);
        for (var turn : command.getTurns()) voiceRecordTurnRepository.save(new VoiceRecordTurn(null, session, turn.getRole(), turn.getContent(),
            turn.getExtractedFields()));
        var transcript = new ArrayList<String>();
        for (var turn : command.getTurns()) transcript.add(turn.getRole() + ": " + turn.getContent());
        session.markWaitingConfirmation(String.join("\n", transcript));
        return new VoiceSessionResult.Processed(required(session.getId()), session.getStatus(), command.getCandidate(),
            VoiceRecordCandidateAnalyzer.missingFields(command.getCandidate()));
    }

    public VoiceSessionResult.Confirmed confirm(VoiceSessionCommand.Confirm command) {
        Objects.requireNonNull(command, "command");
        var session = findSession(command.getSessionId(), command.getMemberId());
        if (session.getStatus() != VoiceSessionStatus.WAITING_CONFIRMATION) throw new BusinessException(ErrorCode.VOICE_SESSION_INVALID_STATE);
        var record = command.getRecord();
        var created = farmingRecordService.create(new FarmingRecordCommand.Create(record.getMemberId(), record.getFarmId(), record.getCropId(),
            record.getWorkType(), record.getWorkedAt(), record.getWeatherCondition(), record.getWeatherTemperature(), record.getMemo(), record.getPlanting(),
            record.getWatering(), record.getFertilizing(), record.getPestControl(), record.getWeeding(), record.getHarvest(), record.getMediaIds(),
                EntryMode.VOICE));
        var saved = farmingRecordRepository.findById(created.getId()).orElseThrow(() -> new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND));
        session.confirm(saved, LocalDateTime.now());
        return new VoiceSessionResult.Confirmed(required(session.getId()), created.getId(), created.getWorkType());
    }

    public VoiceSessionResult.Cancelled cancel(VoiceSessionCommand.Cancel command) {
        Objects.requireNonNull(command, "command");
        var session = findSession(command.getSessionId(), command.getMemberId());
        if (session.getStatus() == VoiceSessionStatus.COMPLETED) throw new BusinessException(ErrorCode.VOICE_SESSION_INVALID_STATE);
        session.cancel();
        return new VoiceSessionResult.Cancelled(required(session.getId()), session.getStatus());
    }

    /** 회원 작물의 농약을 우선하고, 매칭이 없으면 작은 전체 카탈로그만 지침에 넣는다. */
    private List<VoicePesticideOption> loadPesticideOptions(Map<String, List<CropOption>> cropsByFarm) {
        var names = new LinkedHashSet<String>();
        for (var crops : cropsByFarm.values()) for (var crop : crops) names.add(crop.getName());
        var rows = names.isEmpty() ? List.<PesticideQueryRepository.VoiceCatalogRow>of()
            : pesticideQueryRepository.findByCropNames(new ArrayList<>(names), MAX_PESTICIDE_CATALOG_ROWS);
        if (!rows.isEmpty()) {
            var groups = new LinkedHashMap<List<String>, List<PesticideQueryRepository.VoiceCatalogRow>>();
            for (var row : rows) groups.computeIfAbsent(Arrays.asList(row.itemName(), row.brandName()), key -> new ArrayList<>()).add(row);
            var options = new ArrayList<VoicePesticideOption>();
            for (var entry : groups.entrySet()) {
                if (options.size() == MAX_PESTICIDE_OPTIONS) break;
                var pests = new LinkedHashSet<String>();
                for (var row : entry.getValue()) pests.add(row.pestName());
                var values = new ArrayList<>(pests);
                List<String> limited = values.isEmpty() ? List.of() : values.size() == 1 ? Collections.singletonList(values.getFirst())
                    : new ArrayList<>(values.subList(0, Math.min(MAX_PESTS_PER_PESTICIDE, values.size())));
                options.add(new VoicePesticideOption(entry.getKey().get(0) + "(" + entry.getKey().get(1) + ")", limited));
            }
            return options;
        }
        var catalog = pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(null, null, MAX_PESTICIDE_OPTIONS + 1));
        if (catalog.isEmpty() || catalog.size() > MAX_PESTICIDE_OPTIONS) return List.of();
        var options = new ArrayList<VoicePesticideOption>();
        for (var item : catalog) options.add(new VoicePesticideOption(item.getItemName() + "(" + item.getBrandName() + ")", List.of()));
        return options;
    }

    private Member findMember(UUID id) { return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); }
    private VoiceRecordSession findSession(UUID sessionId, UUID memberId) {
        var session = voiceRecordSessionRepository.findByIdAndMemberId(sessionId, memberId);
        if (session == null) throw new BusinessException(ErrorCode.VOICE_SESSION_NOT_FOUND);
        return session;
    }
    private static <T> T required(T value) {
        if (value == null) throw new IllegalArgumentException("Required value was null.");
        return value;
    }
}
