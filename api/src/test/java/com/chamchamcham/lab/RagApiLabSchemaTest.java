package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import static org.assertj.core.api.Assertions.assertThat;

class RagApiLabSchemaTest {
    @Test
    void generated_text_alphabet_avoids_the_raw_quote_and_backslash_that_broke_local_json_generation() throws Exception {
        var schema = new ObjectMapper().readTree(new BeanOutputConverter<>(RecordFeedbackContent.class).getJsonSchema());
        RagApiLabApplication.LabConfiguration.constrainOutput(schema);
        var pattern = Pattern.compile(schema.path("properties").path("goodPoint").path("properties").path("text").path("pattern").asText());
        assertThat(pattern.matcher("토양을 살펴보고 물을 주세요.").matches()).isTrue();
        assertThat(pattern.matcher("물을 주기 전에 \"흙\"을 살펴보세요.").matches()).isFalse();
        assertThat(pattern.matcher("물을 주기 전에 \\흙을 살펴보세요.").matches()).isFalse();
    }
}
