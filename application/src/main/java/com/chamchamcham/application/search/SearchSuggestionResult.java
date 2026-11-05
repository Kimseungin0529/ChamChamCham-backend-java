package com.chamchamcham.application.search;

import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

public final class SearchSuggestionResult {
    private SearchSuggestionResult() {}

    public record Suggestions(
        @NonNull List<String> keywords
    ) {
        public Suggestions {
            Objects.requireNonNull(keywords, "keywords");
        }

        @NonNull
        public List<String> getKeywords() { return keywords; }
    }
}
