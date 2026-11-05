package com.chamchamcham.application.search;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideQueryRepository;
import com.chamchamcham.domain.policy.PolicyProgram;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class SearchSuggestionServiceTest {
    @Mock private CropRepository cropRepository;
    @Mock private PesticideQueryRepository pesticideQueryRepository;
    @Mock private PolicyProgramRepository policyProgramRepository;
    private SearchSuggestionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SearchSuggestionService(cropRepository, pesticideQueryRepository, policyProgramRepository);
    }

    @Test
    @DisplayName("null keyword returns empty list")
    void null_keyword_returns_empty_list() throws Exception {
        assertThat(service.suggest(null).getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("blank keyword returns empty list")
    void blank_keyword_returns_empty_list() throws Exception {
        assertThat(service.suggest("   ").getKeywords()).isEmpty();
    }

    @Test
    @DisplayName("trimmed original keyword is always first and sources merge in round robin order")
    void trimmed_original_keyword_is_always_first_and_sources_merge_in_round_robin_order() throws Exception {
        stubSources("황기", java.util.Arrays.asList("작물1", "작물2"), java.util.Arrays.asList("약제1", "약제2"), java.util.Arrays.asList("정책1", "정책2"), java.util.Arrays.asList("기관1", "기관2"));
        var result = service.suggest("  황기  ").getKeywords();
        assertThat(result).containsExactly("황기", "작물1", "약제1", "정책1", "기관1", "작물2", "약제2", "정책2", "기관2");
    }

    @Test
    @DisplayName("single non-empty source fills the remaining slots")
    void single_non_empty_source_fills_the_remaining_slots() throws Exception {
        stubSources("작물", java.util.stream.IntStream.rangeClosed(1, 9).boxed().map(it -> "작물" + it + "").toList(), List.of(), List.of(), List.of());
        var result = service.suggest("작물").getKeywords();
        assertThat(result).hasSize(10);
        assertThat(result.getFirst()).isEqualTo("작물");
        assertThat(result.subList(1, result.size())).containsExactly("작물1", "작물2", "작물3", "작물4", "작물5", "작물6", "작물7", "작물8", "작물9");
    }

    @Test
    @DisplayName("returns only available candidates when total is under nine")
    void returns_only_available_candidates_when_total_is_under_nine() throws Exception {
        stubSources("인삼", java.util.Arrays.asList("인삼밭"), java.util.Arrays.asList("인삼약"), List.of(), List.of());
        var result = service.suggest("인삼").getKeywords();
        assertThat(result).containsExactly("인삼", "인삼밭", "인삼약");
    }

    @Test
    @DisplayName("deduplicates candidates ignoring case and whitespace keeping first occurrence")
    void deduplicates_candidates_ignoring_case_and_whitespace_keeping_first_occurrence() throws Exception {
        stubSources("과일", java.util.Arrays.asList("Apple"), java.util.Arrays.asList(" apple "), java.util.Arrays.asList("APPLE"), java.util.Arrays.asList("Banana"));
        var result = service.suggest("과일").getKeywords();
        assertThat(result).containsExactly("과일", "Apple", "Banana");
    }

    @Test
    @DisplayName("excludes candidates equal to original keyword from related suggestions")
    void excludes_candidates_equal_to_original_keyword_from_related_suggestions() throws Exception {
        stubSources("사과", java.util.Arrays.asList(" 사과 "), java.util.Arrays.asList("사과약"), List.of(), List.of());
        var result = service.suggest("사과").getKeywords();
        assertThat(result).containsExactly("사과", "사과약");
    }

    @Test
    @DisplayName("total keywords never exceed ten")
    void total_keywords_never_exceed_ten() throws Exception {
        stubSources("농사", java.util.stream.IntStream.rangeClosed(1, 9).boxed().map(it -> "C" + it + "").toList(), java.util.stream.IntStream.rangeClosed(1, 9).boxed().map(it -> "P" + it + "").toList(), java.util.stream.IntStream.rangeClosed(1, 9).boxed().map(it -> "T" + it + "").toList(), java.util.stream.IntStream.rangeClosed(1, 9).boxed().map(it -> "A" + it + "").toList());
        var result = service.suggest("농사").getKeywords();
        assertThat(result).hasSize(10);
        assertThat(result.getFirst()).isEqualTo("농사");
    }
    private void stubSources(String keyword, List<String> crops, List<String> pesticides, List<String> titles, List<String> agencies) {
        when(cropRepository.findTop9ByNameContainingIgnoreCaseOrderByNameAsc(keyword)).thenReturn(crops.stream().map(it -> new Crop(0, it, CropUsePartCategory.ROOT_BARK)).toList());
        when(pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(keyword, null, 9))).thenReturn(pesticides.stream().map(it -> new Pesticide(it, it)).toList());
        when(policyProgramRepository.findTitleSuggestions(keyword, PageRequest.of(0, 9))).thenReturn(titles.stream().map(it -> policyProgram(it, "기관")).toList());
        when(policyProgramRepository.findAgencySuggestions(keyword, PageRequest.of(0, 9))).thenReturn(agencies.stream().map(it -> policyProgram("정책", it)).toList());
    }
    private PolicyProgram policyProgram(String title, String agencyName) {
        var program = new PolicyProgram(title, "정책 상세", "전국", null);
        program.setAgencyName(agencyName);
        return program;
    }
}
