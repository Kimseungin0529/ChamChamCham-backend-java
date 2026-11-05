package com.chamchamcham.application.search;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.community.*;
import com.chamchamcham.application.farming.*;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@Transactional(readOnly = true)
public class SearchService {
    private static final int PREVIEW_SIZE = 3;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private final FarmingRecordService farmingRecordService;
    private final CommunityPostService communityPostService;
    private final PolicyProgramQueryRepository policyProgramQueryRepository;
    private final OpaqueCursorCodec cursorCodec;
    public SearchService(FarmingRecordService farmingRecordService, CommunityPostService communityPostService,
        PolicyProgramQueryRepository policyProgramQueryRepository, OpaqueCursorCodec cursorCodec) {
        this.farmingRecordService = Objects.requireNonNull(farmingRecordService, "farmingRecordService");
        this.communityPostService = Objects.requireNonNull(communityPostService, "communityPostService");
        this.policyProgramQueryRepository = Objects.requireNonNull(policyProgramQueryRepository, "policyProgramQueryRepository");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }
    public SearchResult.All searchAll(UUID memberId, String keyword) throws JsonProcessingException {
        var preview = new SearchQuery(memberId, keyword, null, PREVIEW_SIZE);
        return new SearchResult.All(searchRecords(preview), searchPolicies(preview), searchPosts(preview));
    }
    public SearchResult.RecordPage searchRecords(SearchQuery query) throws JsonProcessingException {
        Objects.requireNonNull(query, "query");
        var condition = new FarmingRecordSearchCondition(query.getMemberId(), List.of(), List.of(), null, null, query.getKeyword(), query.getCursor(), query.getSize());
        var page = farmingRecordService.search(condition);
        return new SearchResult.RecordPage(page.getItems(), page.getNextCursor(), farmingRecordService.count(condition));
    }
    public SearchResult.PostPage searchPosts(SearchQuery query) throws JsonProcessingException {
        Objects.requireNonNull(query, "query");
        var condition = new CommunityPostSearchCondition(query.getMemberId(), null, List.of(), null, query.getKeyword(), false, false, CommunityPostSort.LATEST, query.getCursor(), query.getSize());
        var page = communityPostService.search(condition);
        return new SearchResult.PostPage(page.getItems(), page.getNextCursor(), communityPostService.count(condition));
    }
    public SearchResult.PolicyPage searchPolicies(SearchQuery query) throws JsonProcessingException {
        Objects.requireNonNull(query, "query");
        if (query.getSize() < MIN_PAGE_SIZE || query.getSize() > MAX_PAGE_SIZE) throw new BusinessException(ErrorCode.INVALID_INPUT);
        var cursor = query.getCursor() == null ? null : cursorCodec.decode(query.getCursor(), PolicySearchCursorPayload.class);
        var rows = policyProgramQueryRepository.search(new PolicyProgramQueryRepository.SearchCondition(query.getKeyword(), cursor == null ? null : cursor.getCreatedAt(), cursor == null ? null : cursor.getId(), query.getSize() + 1));
        var visible = new ArrayList<>(rows.subList(0, Math.min(query.getSize(), rows.size())));
        String nextCursor = null;
        if (rows.size() > query.getSize()) {
            var last = visible.getLast();
            if (last.getId() == null) throw new IllegalArgumentException("Required value was null.");
            nextCursor = cursorCodec.encode(new PolicySearchCursorPayload(last.getCreatedAt(), last.getId()));
        }
        var items = new ArrayList<SearchResult.PolicyItem>(visible.size());
        for (var program : visible) items.add(toPolicyItem(program));
        return new SearchResult.PolicyPage(items, nextCursor, policyProgramQueryRepository.count(query.getKeyword()));
    }
    private SearchResult.PolicyItem toPolicyItem(PolicyProgram program) {
        if (program.getId() == null) throw new IllegalArgumentException("Persisted policy program id is required");
        return new SearchResult.PolicyItem(program.getId(), program.getTitle(), program.getAgencyName(), program.getEligibilitySummary(), program.getBenefitSummary(), program.getApplicationPeriodLabel(), program.getApplyStartsOn(), program.getApplyEndsOn(), program.getSourceUrl(), program.getCreatedAt());
    }
}
