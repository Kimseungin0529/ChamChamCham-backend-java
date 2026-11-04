package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Component
public class CoachingContextProvider {
    private final MemberRepository memberRepository;
    private final FarmRepository farmRepository;
    private final CropRepository cropRepository;
    private final FarmingRecordRepository farmingRecordRepository;
    public CoachingContextProvider(MemberRepository memberRepository, FarmRepository farmRepository, CropRepository cropRepository, FarmingRecordRepository farmingRecordRepository) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository"); this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository"); this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository"); this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
    }
    @Transactional(readOnly = true)
    public CoachingContext build(CoachingRagCommand command) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findById(command.getMemberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var farm = command.getFarmId() == null ? null : farmRepository.findByIdAndOwnerId(command.getFarmId(), command.getMemberId());
        if (command.getFarmId() != null && farm == null) throw new BusinessException(ErrorCode.RAG_INVALID_REQUEST);
        var crop = command.getCropId() == null ? null : cropRepository.findById(command.getCropId()).orElse(null);
        var record = command.getRecordId() == null ? null : farmingRecordRepository.findByIdAndMember_Id(command.getRecordId(), command.getMemberId());
        if (command.getRecordId() != null && record == null) throw new BusinessException(ErrorCode.RAG_INVALID_REQUEST);
        var text = new StringBuilder("사용자 재배 context:\n");
        text.append("- 영농 경력 점수: ").append(member.getExperienceLevel() == null ? "미입력" : member.getExperienceLevel().toString()).append('\n');
        text.append("- 경영 형태: ").append(member.getManagementType() == null ? "미입력" : member.getManagementType().name()).append('\n');
        if (farm != null) text.append("- 농장: ").append(farm.getName()).append(" (").append(farm.getRoadAddress()).append(")\n");
        if (crop != null) text.append("- 작물: ").append(crop.getName()).append(" / ").append(crop.getUsePartCategory().getLabel()).append('\n');
        if (record != null) text.append("- 기준 영농일지: ").append(record.getWorkedAt()).append(' ').append(record.getWorkType().getLabel()).append('\n');
        if (command.getPeriodStart() != null) text.append("- 기간 시작: ").append(command.getPeriodStart()).append('\n');
        if (command.getPeriodEnd() != null) text.append("- 기간 종료: ").append(command.getPeriodEnd()).append('\n');
        return new CoachingContext(trim(text.toString()));
    }
    private static String trim(String text) {
        int start = 0, end = text.length();
        while (start < end && (Character.isWhitespace(text.charAt(start)) || Character.isSpaceChar(text.charAt(start)))) start++;
        while (end > start && (Character.isWhitespace(text.charAt(end - 1)) || Character.isSpaceChar(text.charAt(end - 1)))) end--;
        return text.substring(start, end);
    }
}
