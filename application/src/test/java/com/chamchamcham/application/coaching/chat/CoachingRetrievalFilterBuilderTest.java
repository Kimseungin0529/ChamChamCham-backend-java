package com.chamchamcham.application.coaching.chat;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContextKt;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class CoachingRetrievalFilterBuilderTest {
    private final CoachingRetrievalFilterBuilder builder = new CoachingRetrievalFilterBuilder();

    @Test
    @DisplayName("build restricts generic chat retrieval to technical documents")
    void build_restricts_generic_chat_retrieval_to_technical_documents() throws Exception {
        assertThat(builder.build()).isEqualTo("sourceType == 'TECH_DOCUMENT'");
    }
}
