package com.chamchamcham.application.search;

import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.pesticide.PesticideQueryRepository;
import com.chamchamcham.domain.policy.PolicyProgramRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SearchSuggestionService {
    private static final int MAX_RELATED = 9;
    private final CropRepository cropRepository;
    private final PesticideQueryRepository pesticideQueryRepository;
    private final PolicyProgramRepository policyProgramRepository;
    public SearchSuggestionService(CropRepository cropRepository, PesticideQueryRepository pesticideQueryRepository, PolicyProgramRepository policyProgramRepository) {
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.pesticideQueryRepository = Objects.requireNonNull(pesticideQueryRepository, "pesticideQueryRepository");
        this.policyProgramRepository = Objects.requireNonNull(policyProgramRepository, "policyProgramRepository");
    }
    public SearchSuggestionResult.Suggestions suggest(String keyword) {
        String trimmed = keyword == null ? "" : trim(keyword);
        if (trimmed.isEmpty()) return new SearchSuggestionResult.Suggestions(List.of());
        var sources = List.of(
            cropRepository.findTop9ByNameContainingIgnoreCaseOrderByNameAsc(trimmed).stream().map(crop -> crop.getName()).toList(),
            pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(trimmed, null, MAX_RELATED)).stream().map(pesticide -> pesticide.getBrandName()).toList(),
            policyProgramRepository.findTitleSuggestions(trimmed, PageRequest.of(0, MAX_RELATED)).stream().map(policy -> policy.getTitle()).toList(),
            policyProgramRepository.findAgencySuggestions(trimmed, PageRequest.of(0, MAX_RELATED)).stream().map(policy -> policy.getAgencyName()).toList());
        var related = collectRoundRobin(sources, normalize(trimmed));
        var result = new ArrayList<String>(related.size() + 1);
        result.add(trimmed);
        result.addAll(related);
        return new SearchSuggestionResult.Suggestions(result);
    }
    private List<String> collectRoundRobin(List<List<String>> sources, String originalKey) {
        int[] cursors = new int[sources.size()];
        var seen = new HashSet<String>();
        seen.add(originalKey);
        var result = new ArrayList<String>(MAX_RELATED);
        while (result.size() < MAX_RELATED) {
            boolean advanced = false;
            for (int i = 0; i < sources.size(); i++) {
                if (result.size() >= MAX_RELATED) break;
                var source = sources.get(i);
                while (cursors[i] < source.size()) {
                    String candidate = source.get(cursors[i]++);
                    if (seen.add(normalize(candidate))) {
                        result.add(candidate);
                        advanced = true;
                        break;
                    }
                }
            }
            if (!advanced) break;
        }
        return result;
    }
    private String normalize(String value) { return trim(value).toLowerCase(Locale.ROOT); }
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
