package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.RagSourceType;
import org.springframework.stereotype.Component;

@Component
public class CoachingRetrievalFilterBuilder {
    public String build() { return "sourceType == '" + RagSourceType.TECH_DOCUMENT.name() + "'"; }
}
