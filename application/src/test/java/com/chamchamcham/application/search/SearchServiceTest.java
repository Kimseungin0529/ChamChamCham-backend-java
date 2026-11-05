package com.chamchamcham.application.search;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.community.CommunityPostResult;
import com.chamchamcham.application.community.CommunityPostSearchCondition;
import com.chamchamcham.application.community.CommunityPostService;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.farming.FarmingRecordResult;
import com.chamchamcham.application.farming.FarmingRecordSearchCondition;
import com.chamchamcham.application.farming.FarmingRecordService;
import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.chamchamcham.domain.community.CommunityPostType;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramQueryRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class SearchServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 9, 0);
    @Mock private FarmingRecordService farmingRecordService;
    @Mock private CommunityPostService communityPostService;
    @Mock private PolicyProgramQueryRepository policyProgramQueryRepository;
    private final OpaqueCursorCodec cursorCodec = new OpaqueCursorCodec();
    private SearchService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SearchService(farmingRecordService, communityPostService, policyProgramQueryRepository, cursorCodec);
    }

    @Test
    @DisplayName("searchRecords delegates keyword condition to farming record service with count")
    void searchRecords_delegates_keyword_condition_to_farming_record_service_with_count() throws Exception {
        var condition = recordCondition("황기", "cursor-1", 10);
        var summary = recordSummary();
        when(farmingRecordService.search(condition)).thenReturn(new FarmingRecordResult.Page(java.util.Arrays.asList(summary), "cursor-2"));
        when(farmingRecordService.count(condition)).thenReturn(25L);
        var page = service.searchRecords(new SearchQuery(memberId, "황기", "cursor-1", 10));
        assertThat(page.getItems()).containsExactly(summary);
        assertThat(page.getNextCursor()).isEqualTo("cursor-2");
        assertThat(page.getTotalCount()).isEqualTo(25);
    }

    @Test
    @DisplayName("searchPosts delegates latest-sorted keyword condition to community post service with count")
    void searchPosts_delegates_latest_sorted_keyword_condition_to_community_post_service_with_count() throws Exception {
        var condition = postCondition("황기", null, 20);
        var summary = postSummary();
        when(communityPostService.search(condition)).thenReturn(new CommunityPostResult.Page(java.util.Arrays.asList(summary), null));
        when(communityPostService.count(condition)).thenReturn(1L);
        var page = service.searchPosts(new SearchQuery(memberId, "황기", null, 20));
        assertThat(page.getItems()).containsExactly(summary);
        assertThat(page.getNextCursor()).isNull();
        assertThat(page.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchPolicies maps programs and encodes next cursor only when an extra row exists")
    void searchPolicies_maps_programs_and_encodes_next_cursor_only_when_an_extra_row_exists() throws Exception {
        var programs = java.util.stream.IntStream.rangeClosed(1, 3).boxed().map(index -> policyProgram("정책 " + index + "", createdAt.minusDays(index.longValue()))).toList();
        when(policyProgramQueryRepository.search(new PolicyProgramQueryRepository.SearchCondition("정책", null, null, 3))).thenReturn(programs);
        when(policyProgramQueryRepository.count("정책")).thenReturn(10L);
        var page = service.searchPolicies(new SearchQuery(memberId, "정책", null, 2));
        assertThat(page.getItems()).hasSize(2);
        assertThat(page.getItems().stream().map(it -> it.getTitle()).toList()).containsExactly("정책 1", "정책 2");
        assertThat(page.getTotalCount()).isEqualTo(10);
        var payload = cursorCodec.decode(Objects.requireNonNull(page.getNextCursor()), PolicySearchCursorPayload.class);
        assertEquals(programs.get(1).getCreatedAt(), payload.getCreatedAt());
        assertEquals(programs.get(1).getId(), payload.getId());
    }

    @Test
    @DisplayName("searchPolicies returns null next cursor when rows fit in one page")
    void searchPolicies_returns_null_next_cursor_when_rows_fit_in_one_page() throws Exception {
        var program = policyProgram("정책", createdAt);
        when(policyProgramQueryRepository.search(new PolicyProgramQueryRepository.SearchCondition(null, null, null, 21))).thenReturn(java.util.Arrays.asList(program));
        when(policyProgramQueryRepository.count(null)).thenReturn(1L);
        var page = service.searchPolicies(new SearchQuery(memberId, null, null, 20));
        assertThat(page.getItems().get(0).getTitle()).isEqualTo("정책");
        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("searchPolicies decodes cursor into created at and id condition")
    void searchPolicies_decodes_cursor_into_created_at_and_id_condition() throws Exception {
        var cursorProgram = policyProgram("커서 기준", createdAt);
        var cursor = cursorCodec.encode(new PolicySearchCursorPayload(cursorProgram.getCreatedAt(), Objects.requireNonNull(cursorProgram.getId())));
        when(policyProgramQueryRepository.search(new PolicyProgramQueryRepository.SearchCondition(null, cursorProgram.getCreatedAt(), cursorProgram.getId(), 21))).thenReturn(List.of());
        when(policyProgramQueryRepository.count(null)).thenReturn(0L);
        var page = service.searchPolicies(new SearchQuery(memberId, null, cursor, 20));
        assertThat(page.getItems()).isEmpty();
        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("searchPolicies throws for a malformed cursor")
    void searchPolicies_throws_for_a_malformed_cursor() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.searchPolicies(new SearchQuery(memberId, null, "%%%", 20)));
        assertEquals(ErrorCode.INVALID_CURSOR, exception.getErrorCode());
    }

    @Test
    @DisplayName("searchPolicies rejects out-of-range page sizes")
    void searchPolicies_rejects_out_of_range_page_sizes() throws Exception {
        java.util.Arrays.asList(0, 51).forEach(size -> {
        var exception = assertThrows(BusinessException.class, () -> service.searchPolicies(new SearchQuery(memberId, null, null, size)));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        });
    }

    @Test
    @DisplayName("searchAll assembles three preview sections with fixed size and no cursor")
    void searchAll_assembles_three_preview_sections_with_fixed_size_and_no_cursor() throws Exception {
        var recordConditionPreview = recordCondition("황기", null, 3);
        var postConditionPreview = postCondition("황기", null, 3);
        var summary = recordSummary();
        var post = postSummary();
        var program = policyProgram("황기 재배 지원", createdAt);
        when(farmingRecordService.search(recordConditionPreview)).thenReturn(new FarmingRecordResult.Page(java.util.Arrays.asList(summary), "more"));
        when(farmingRecordService.count(recordConditionPreview)).thenReturn(12L);
        when(communityPostService.search(postConditionPreview)).thenReturn(new CommunityPostResult.Page(java.util.Arrays.asList(post), null));
        when(communityPostService.count(postConditionPreview)).thenReturn(1L);
        when(policyProgramQueryRepository.search(new PolicyProgramQueryRepository.SearchCondition("황기", null, null, 4))).thenReturn(java.util.Arrays.asList(program));
        when(policyProgramQueryRepository.count("황기")).thenReturn(1L);
        var all = service.searchAll(memberId, "황기");
        assertThat(all.getRecords().getItems()).containsExactly(summary);
        assertThat(all.getRecords().getTotalCount()).isEqualTo(12);
        assertThat(all.getPolicies().getItems().get(0).getTitle()).isEqualTo("황기 재배 지원");
        assertThat(all.getPolicies().getTotalCount()).isEqualTo(1);
        assertThat(all.getPosts().getItems()).containsExactly(post);
        assertThat(all.getPosts().getTotalCount()).isEqualTo(1);
    }
    private FarmingRecordSearchCondition recordCondition(String keyword, String cursor, int size) {
        return new FarmingRecordSearchCondition(memberId, List.of(), List.of(), null, null, keyword, cursor, size);
    }
    private CommunityPostSearchCondition postCondition(String keyword, String cursor, int size) {
        return new CommunityPostSearchCondition(memberId, null, List.of(), null, keyword, false, false, CommunityPostSort.LATEST, cursor, size);
    }
    private FarmingRecordResult.Summary recordSummary() {
        return new FarmingRecordResult.Summary(UUID.fromString("00000000-0000-0000-0000-000000000101"), UUID.fromString("00000000-0000-0000-0000-000000000201"), "황기", WorkType.HARVEST, createdAt, "맑음", 23, "수확 완료", null, null, null, null, null, null, null);
    }
    private CommunityPostResult.PostSummary postSummary() {
        return new CommunityPostResult.PostSummary(UUID.fromString("00000000-0000-0000-0000-000000000301"), UUID.fromString("00000000-0000-0000-0000-000000000201"), "황기", CommunityPostType.GENERAL, "황기 수확 후기", "올해 수확량이", null, new CommunityPostResult.AuthorSummary(memberId, "농부", null), 2, 5, false, createdAt);
    }
    private PolicyProgram policyProgram(String title, LocalDateTime createdAt) {
        var program = new PolicyProgram(UUID.randomUUID(), title, "정책 상세", "전국", null);
        try {
            var field = BaseTimeEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(program, createdAt);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        return program;
    }
}
