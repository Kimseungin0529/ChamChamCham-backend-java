package com.chamchamcham.api.search.dto;

import com.chamchamcham.application.search.SearchSuggestionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class SearchSuggestionResponses {
    private SearchSuggestionResponses() {}

    @Schema(description = "연관 검색어 응답")
    public record SuggestionsResponse(
        @Schema(
        description = "연관 검색어 목록. 첫 번째 요소는 입력한 검색어 자신이며, " +
        "이후 작물명·농약 상표명·정책 제목·기관명에서 수집한 연관어가 최대 9건 이어집니다",
        example = "[\"황기\", \"황기차\", \"황기 재배 지원\"]"
        )
        @NonNull List<String> keywords
    ) {
        public SuggestionsResponse {
            Objects.requireNonNull(keywords, "keywords");
        }

        @NonNull public List<String> getKeywords() { return keywords; }

        public static SuggestionsResponse from(SearchSuggestionResult.Suggestions result) {
            return new SuggestionsResponse(
                result.getKeywords()
            );
        }
    }
}
