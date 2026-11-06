package com.chamchamcham.api.coaching.recordfeedback;

import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.coaching.common.RagSourceType;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackGenerationService;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackRetrievalQueryPlanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("local-smoke")
@ActiveProfiles("local")
@SpringBootTest(classes = ApiApplication.class)
@EnabledIfEnvironmentVariable(named = "RUN_LOCAL_RAG_SMOKE", matches = "true")
class RecordFeedbackGenerationVectorStoreSmokeTest {
    @Autowired private VectorStore vectorStore;
    @Autowired private RagProperties ragProperties;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final RecordFeedbackRetrievalQueryPlanner planner = new RecordFeedbackRetrievalQueryPlanner();

    @Test
    @DisplayName("fake watering payload retrieves real tech document chunks from vector store")
    void fakeWateringPayloadRetrievesRealTechDocumentChunksFromVectorStore() throws Exception {
        var context = readFixture("today-record-feedback-watering.json");
        var queries = planner.plan(context);
        var cropName = context.getCrop().getName().replaceAll("^[\\p{javaWhitespace}\\p{Zs}]+|[\\p{javaWhitespace}\\p{Zs}]+$", "");
        var retrievedById = new LinkedHashMap<String, Document>();
        for (var query : queries) {
            for (var document : vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query.getQuery()).topK(3)
                    .similarityThreshold(ragProperties.getRetrieval().getLowSimilarityThreshold())
                    .filterExpression("sourceType == '" + RagSourceType.TECH_DOCUMENT.name() + "' && "
                            + "cropName in ['" + cropName + "', '" + RecordFeedbackGenerationService.GENERAL_CROP_NAME + "']")
                    .build())) {
                retrievedById.putIfAbsent(document.getId(), document);
            }
        }
        assertThat(queries).extracting(query -> query.getQuery()).contains("참당귀 관수 재배 관리 약용작물");
        assertThat(retrievedById.values())
                .withFailMessage("Import verified TECH_DOCUMENT vectors into public.vector_store before running this smoke test.")
                .isNotEmpty();
        assertThat(retrievedById.values()).allSatisfy(document -> {
            assertThat(document.getMetadata().get("sourceType")).isEqualTo(RagSourceType.TECH_DOCUMENT.name());
            assertThat(document.getText()).isNotBlank();
        });
    }

    private RecordFeedbackContext readFixture(String name) throws Exception {
        return objectMapper.readValue(Files.readString(Path.of("application/src/test/resources/coaching/rag", name)), RecordFeedbackContext.class);
    }
}
