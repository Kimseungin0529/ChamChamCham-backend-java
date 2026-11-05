package com.chamchamcham.application.policy.recommendation;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.policy.support.*;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farm.*;
import com.chamchamcham.domain.member.*;
import com.chamchamcham.domain.policy.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class PolicyRecommendationService {
    private static final int COUNT_QUERY_SIZE = 1;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PolicySyncJobRepository policySyncJobRepository;
    private final PolicyProgramRepository policyProgramRepository;
    private final PolicyRecommendationRepository policyRecommendationRepository;
    private final PolicyRecommendationQueryRepository policyRecommendationQueryRepository;
    private final MemberRepository memberRepository;
    private final MemberCropRepository memberCropRepository;
    private final FarmRepository farmRepository;
    private final PolicyRegionMatcher regionMatcher;
    private final PolicyRecommendationScorer scorer;
    private final TextListJsonCodec textListJsonCodec;
    private final OpaqueCursorCodec cursorCodec;
    private final Clock clock;
    @Autowired(required = false)
    public PolicyRecommendationService(PolicySyncJobRepository policySyncJobRepository, PolicyProgramRepository policyProgramRepository, PolicyRecommendationRepository policyRecommendationRepository, PolicyRecommendationQueryRepository policyRecommendationQueryRepository, MemberRepository memberRepository, MemberCropRepository memberCropRepository, FarmRepository farmRepository, PolicyRegionMatcher regionMatcher, PolicyRecommendationScorer scorer, TextListJsonCodec textListJsonCodec, OpaqueCursorCodec cursorCodec) {
        this(policySyncJobRepository, policyProgramRepository, policyRecommendationRepository, policyRecommendationQueryRepository, memberRepository, memberCropRepository, farmRepository, regionMatcher, scorer, textListJsonCodec, cursorCodec, Clock.systemDefaultZone());
    }
    @Autowired(required = false)
    public PolicyRecommendationService(PolicySyncJobRepository policySyncJobRepository, PolicyProgramRepository policyProgramRepository, PolicyRecommendationRepository policyRecommendationRepository, PolicyRecommendationQueryRepository policyRecommendationQueryRepository, MemberRepository memberRepository, MemberCropRepository memberCropRepository, FarmRepository farmRepository, PolicyRegionMatcher regionMatcher, PolicyRecommendationScorer scorer, TextListJsonCodec textListJsonCodec, OpaqueCursorCodec cursorCodec, Clock clock) {
        this.policySyncJobRepository = Objects.requireNonNull(policySyncJobRepository, "policySyncJobRepository");
        this.policyProgramRepository = Objects.requireNonNull(policyProgramRepository, "policyProgramRepository");
        this.policyRecommendationRepository = Objects.requireNonNull(policyRecommendationRepository, "policyRecommendationRepository");
        this.policyRecommendationQueryRepository = Objects.requireNonNull(policyRecommendationQueryRepository, "policyRecommendationQueryRepository");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.farmRepository = Objects.requireNonNull(farmRepository, "farmRepository");
        this.regionMatcher = Objects.requireNonNull(regionMatcher, "regionMatcher");
        this.scorer = Objects.requireNonNull(scorer, "scorer");
        this.textListJsonCodec = Objects.requireNonNull(textListJsonCodec, "textListJsonCodec");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
        this.clock = Objects.requireNonNull(clock, "clock");
    }
    public PolicyRecommendationResult.Page listRecommendations(UUID memberId, String cursor, int size) throws JsonProcessingException {
        return listRecommendations(memberId, cursor, size, null, PolicyRecommendationSort.RECOMMENDED);
    }
    public PolicyRecommendationResult.Page listRecommendations(UUID memberId, String cursor, int size, PolicyBenefitCategory benefitCategory) throws JsonProcessingException {
        return listRecommendations(memberId, cursor, size, benefitCategory, PolicyRecommendationSort.RECOMMENDED);
    }
    public PolicyRecommendationResult.Page listRecommendations(UUID memberId, String cursor, int size, PolicyBenefitCategory benefitCategory, PolicyRecommendationSort sort) throws JsonProcessingException {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(sort, "sort");
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.INVALID_INPUT);
        var latestJob = policySyncJobRepository.findFirstBySourceAndStatusOrderByTargetYearDescFinishedAtDesc(PolicySource.NONGUP_EZ, PolicySyncJobStatus.SUCCEEDED);
        if (latestJob == null) return new PolicyRecommendationResult.Page(List.of(), null);
        var source = PolicySource.NONGUP_EZ;
        String sourceYear = latestJob.getTargetYear();
        var decodedCursor = decodeCursor(cursor, source, sourceYear, benefitCategory, sort);
        var today = LocalDate.now(clock);
        var candidates = policyProgramRepository.findRecommendableCandidates(source, sourceYear, today);
        var member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var farms = farmRepository.findAllWithBoundaryCoordinatesByOwnerId(memberId);
        var memberCrops = memberCropRepository.findAllWithCropByMemberId(memberId);
        if (recommendationsAreStale(memberId, source, sourceYear, candidates, member, memberCrops, farms)) regenerate(memberId, member, source, sourceYear, candidates, today, memberCrops, farms);
        var result = policyRecommendationQueryRepository.findPage(new PolicyRecommendationQueryRepository.SearchCondition(memberId, source, sourceYear,
            benefitCategory == null ? null : benefitCategory.getLabel(), sort, decodedCursor, size + 1));
        var visible = new ArrayList<>(result.rows().subList(0, Math.min(size, result.rows().size())));
        String nextCursor = result.rows().size() > size && !visible.isEmpty() ? encodeCursor(visible.getLast(), source, sourceYear, benefitCategory, sort) : null;
        var cards = new ArrayList<PolicyRecommendationResult.Card>(visible.size());
        for (var row : visible) cards.add(toCard(row));
        return new PolicyRecommendationResult.Page(cards, nextCursor);
    }
    public PolicyRecommendationResult.Detail getProgramDetail(UUID memberId, UUID policyProgramId) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(policyProgramId, "policyProgramId");
        if (!memberRepository.existsById(memberId)) throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        var program = policyProgramRepository.findByIdAndDetailSyncedTrueAndRecommendableTrue(policyProgramId);
        if (program == null) throw new BusinessException(ErrorCode.POLICY_PROGRAM_NOT_FOUND, policyProgramId);
        return new PolicyRecommendationResult.Detail(required(program.getId()), program.getTitle(), program.getSourceYear(), program.getAgencyName(), program.getDepartmentName(),
            program.getApplicationPeriodLabel(), program.getOnlineApplyAvailable(), program.getSourceUrl(), program.getApplicationUrl(), program.getPurpose(), program.getSummary(),
            program.getEligibilityOriginal(), program.getBenefitOriginal(), program.getApplicationMethod(), program.getRequiredDocuments(), program.getSelectionCriteria(), parseContacts(program.getRawPayload()), parseAttachments(program.getRawPayload()));
    }
    private boolean recommendationsAreStale(UUID memberId, PolicySource source, String sourceYear, List<PolicyProgram> candidates, Member member, List<MemberCrop> memberCrops, List<Farm> farms) {
        var candidateIds = new HashSet<UUID>();
        for (var program : candidates) candidateIds.add(required(program.getId()));
        var recommendationIds = new HashSet<>(policyRecommendationRepository.findPolicyProgramIdsByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, source, sourceYear));
        var newest = policyRecommendationRepository.findNewestCreatedAtByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, source, sourceYear);
        if (newest == null) return true;
        if (candidateIds.isEmpty()) return !recommendationIds.isEmpty();
        return recommendationIds.isEmpty() || recommendationIds.stream().anyMatch(id -> !candidateIds.contains(id)) ||
            candidates.stream().anyMatch(program -> updatedAfter(program, newest)) || updatedAfter(member, newest) ||
            memberCrops.stream().anyMatch(crop -> updatedAfter(crop, newest)) || farms.stream().anyMatch(farm -> updatedAfter(farm, newest));
    }
    private void regenerate(UUID memberId, Member member, PolicySource source, String sourceYear, List<PolicyProgram> candidates, LocalDate today, List<MemberCrop> memberCrops, List<Farm> farms) {
        var profile = buildPolicyMemberProfile(member, memberCrops, farms);
        policyRecommendationRepository.deleteByMemberIdAndPolicyProgramSourceAndSourceYear(memberId, source, sourceYear);
        var recommendations = new ArrayList<PolicyRecommendation>();
        for (var program : candidates) {
            var score = scorer.score(profile, textListJsonCodec.decode(program.getTargetTagsJson()), textListJsonCodec.decode(program.getCropTagsJson()),
                textListJsonCodec.decode(program.getRegionTagsJson()), program.getOnlineApplyAvailable(), today);
            if (score.getEligible()) recommendations.add(new PolicyRecommendation(member, program, score.getScore(), score.getReason()));
        }
        if (!recommendations.isEmpty()) policyRecommendationRepository.saveAll(recommendations);
    }
    private PolicyMemberProfile buildPolicyMemberProfile(Member member, List<MemberCrop> memberCrops, List<Farm> farms) {
        var names = new ArrayList<String>();
        var categories = new ArrayList<String>();
        for (var memberCrop : memberCrops) { names.add(memberCrop.getCrop().getName()); categories.add(memberCrop.getCrop().getUsePartCategory().name()); }
        var addresses = new ArrayList<String>();
        for (var farm : farms) { addresses.add(farm.getRoadAddress()); addresses.add(farm.getJibunAddress()); }
        return new PolicyMemberProfile(member.getBirthDate(), member.getExperienceLevel(), member.getManagementType(), toSet(names), toSet(categories), regionMatcher.extractRegionTokens(addresses));
    }
    private static <T> Set<T> toSet(List<T> values) {
        if (values.isEmpty()) return Set.of();
        if (values.size() == 1) return Collections.singleton(values.getFirst());
        return new LinkedHashSet<>(values);
    }
    private PolicyRecommendationQueryRepository.Cursor decodeCursor(String cursor, PolicySource source, String sourceYear, PolicyBenefitCategory benefitCategory, PolicyRecommendationSort sort) {
        if (cursor == null || trim(cursor).isEmpty()) return null;
        PolicyRecommendationCursorPayload payload;
        try { payload = cursorCodec.decode(cursor, PolicyRecommendationCursorPayload.class); }
        catch (BusinessException e) { if (e.getErrorCode() == ErrorCode.INVALID_CURSOR) throw new BusinessException(ErrorCode.INVALID_INPUT); throw e; }
        if (payload.getSource() != source || !payload.getSourceYear().equals(sourceYear) || !Objects.equals(payload.getBenefitCategory(), benefitCategory == null ? null : benefitCategory.name()) || payload.getSort() != sort) throw new BusinessException(ErrorCode.INVALID_INPUT);
        if (sort == PolicyRecommendationSort.RECOMMENDED) {
            if (payload.getScore() == null) throw new BusinessException(ErrorCode.INVALID_INPUT);
            return new PolicyRecommendationQueryRepository.Cursor(payload.getScore(), null, payload.getApplyEndsOn(), payload.getId());
        }
        return new PolicyRecommendationQueryRepository.Cursor(null, payload.getApplyStartsOn(), null, payload.getId());
    }
    private String encodeCursor(PolicyRecommendation row, PolicySource source, String year, PolicyBenefitCategory benefitCategory, PolicyRecommendationSort sort) throws JsonProcessingException {
        return cursorCodec.encode(new PolicyRecommendationCursorPayload(source, year, benefitCategory == null ? null : benefitCategory.name(), sort,
            sort == PolicyRecommendationSort.RECOMMENDED ? row.getScore() : null, sort == PolicyRecommendationSort.LATEST ? row.getPolicyProgram().getApplyStartsOn() : null,
            sort == PolicyRecommendationSort.RECOMMENDED ? row.getPolicyProgram().getApplyEndsOn() : null, required(row.getId())));
    }
    private PolicyRecommendationResult.Card toCard(PolicyRecommendation row) {
        var program = row.getPolicyProgram();
        return new PolicyRecommendationResult.Card(required(row.getId()), required(program.getId()), program.getTitle(), program.getEligibilitySummary(), program.getBenefitSummary(), program.getApplicationPeriodLabel(), program.getAgencyName(), row.getScore(), row.getReason(), row.getCreatedAt());
    }
    private List<PolicyRecommendationResult.Contact> parseContacts(String rawPayload) {
        var root = parseRawPayload(rawPayload);
        if (root == null) return List.of();
        var contacts = new ArrayList<PolicyRecommendationResult.Contact>();
        for (var contact : arrayField(root, "bizPicList", "contacts")) {
            String agency = nullableText(contact, "bizTkcgInstCdNm", "instNm", "agencyName");
            String department = nullableText(contact, "bizTkcgDeptInstCdNm", "deptNm", "departmentName", "department");
            String phone = nullableText(contact, "bizPicTelno", "telno", "phoneNumber", "phone");
            if (agency != null || department != null || phone != null) contacts.add(new PolicyRecommendationResult.Contact(agency, department, phone));
        }
        return contacts;
    }
    private List<PolicyRecommendationResult.Attachment> parseAttachments(String rawPayload) {
        var root = parseRawPayload(rawPayload);
        if (root == null) return List.of();
        var attachments = new ArrayList<PolicyRecommendationResult.Attachment>();
        for (var attachment : arrayField(root, "bizAtchFileList", "attachments")) {
            String file = nullableText(attachment, "originalName", "atchFileNm", "fileName", "name");
            String extension = nullableText(attachment, "extension", "fileExtnNm");
            Long size = null;
            for (String field : List.of("size", "fileSz", "sizeBytes")) if (attachment.path(field).isNumber()) { size = attachment.path(field).asLong(); break; }
            String url = nullableText(attachment, "url");
            if (file != null || extension != null || size != null || url != null) attachments.add(new PolicyRecommendationResult.Attachment(file, extension, size, url));
        }
        return attachments;
    }
    private JsonNode parseRawPayload(String raw) {
        try { var root = objectMapper.readTree(raw); var result = root.path("result"); return !result.isMissingNode() && !result.isNull() ? result : root; }
        catch (Throwable e) { return null; }
    }
    private String nullableText(JsonNode node, String... fields) {
        for (String field : fields) { var value = node.path(field); if (value.isTextual()) { String text = trim(value.asText()); if (!text.isEmpty()) return text; } }
        return null;
    }
    private Iterable<JsonNode> arrayField(JsonNode node, String... fields) {
        for (String field : fields) if (node.path(field).isArray()) return node.path(field);
        return List.of();
    }
    private boolean updatedAfter(BaseTimeEntity entity, LocalDateTime threshold) {
        try { return entity.getUpdatedAt().isAfter(threshold); } catch (Throwable e) { return false; }
    }
    private static <T> T required(T value) { if (value == null) throw new IllegalArgumentException("Required value was null."); return value; }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
