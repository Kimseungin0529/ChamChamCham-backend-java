package com.chamchamcham.lab;

import java.util.*;
import java.nio.file.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.chamchamcham.application.coaching.recordfeedback.generation.RecordFeedbackContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import static org.assertj.core.api.Assertions.*;

class CoachingRetrievalLabTest {
    @Test void onlyDedicatedLocalEndpointsAreAccepted() {
        CoachingRetrievalLab.validateTarget("jdbc:postgresql://127.0.0.1:15434/ccc_coaching_retrieval", "http://127.0.0.1:11437");
        for (String url : List.of("jdbc:postgresql://127.0.0.1:15433/ccc_rag_api", "jdbc:postgresql://example.com:15434/ccc_coaching_retrieval"))
            assertThatThrownBy(() -> CoachingRetrievalLab.validateTarget(url,"http://127.0.0.1:11437")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoachingRetrievalLab.validateTarget("jdbc:postgresql://127.0.0.1:15434/ccc_coaching_retrieval","http://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void foreignDatabaseTokenAndUnexpectedTableStopInitialization() {
        assertThat(CoachingRetrievalLab.databaseMode("a","a",List.of("lab_identity"),List.of("public"))).isEqualTo("EMPTY");
        assertThatThrownBy(() -> CoachingRetrievalLab.databaseMode("b","a",List.of("lab_identity"),List.of("public"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoachingRetrievalLab.databaseMode("a","a",List.of("member"),List.of("public"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoachingRetrievalLab.databaseMode("a","a",List.of("lab_identity"),List.of("public","other"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void emptyEvidenceRemainsMissingInsteadOfBecomingSuccess() {
        var need = new CoachingRetrievalLab.Need("source","수분 확인");
        var result=CoachingRetrievalLab.measure(List.of(),List.of(need),Set.of());
        assertThat(result.required()).isEqualTo(1);
        assertThat(result.recovered()).isZero();
        assertThat(result.documents()).isZero();
    }

    @Test void sourceAndBodyMustBothMatchRequiredEvidence() {
        var docs=List.of(new Document("1","수분 확인",Map.of("sourceId","wrong")),new Document("2","다른 문장",Map.of("sourceId","right")));
        assertThat(CoachingRetrievalLab.measure(docs,List.of(new CoachingRetrievalLab.Need("right","수분 확인")),Set.of()).recovered()).isZero();
    }

    @Test void duplicateNeedIsNotCountedTwiceAndForbiddenCandidateIsVisible() {
        var doc=new Document("1","수분 확인",Map.of("sourceId","right"));
        var need=new CoachingRetrievalLab.Need("right","수분 확인");
        var result=CoachingRetrievalLab.measure(List.of(doc),List.of(need,need),Set.of("1"));
        assertThat(result.required()).isEqualTo(1);
        assertThat(result.recovered()).isEqualTo(1);
        assertThat(result.forbidden()).isEqualTo(1);
    }

    @Test void noDeclaredNeedDoesNotInventRetrievalSuccess() {
        var result=CoachingRetrievalLab.measure(List.of(new Document("1","일반 자료",Map.of())),List.of(),Set.of());
        assertThat(result.required()).isZero();
        assertThat(result.recovered()).isZero();
    }

    @Test void sourceBindingMismatchAndConflictingIdCannotEnterCorpus() {
        var first=new Document("1","원문",Map.of("sourceId","source"));
        assertThatThrownBy(() -> CoachingRetrievalLab.uniqueDocuments(List.of(first,new Document("1","다른본문",first.getMetadata()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CoachingRetrievalLab.uniqueDocuments(List.of(first,first))).containsExactly(first);
    }

    @Test void partialVersionTablesCannotBeSilentlyReinitialized() {
        assertThatThrownBy(() -> CoachingRetrievalLab.databaseMode("a","a",List.of("lab_identity","baseline"),List.of("public")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void sameDocumentFromDifferentQueriesCanHaveDifferentDistance() {
        var first=new Document("1","원문",Map.of("sourceId","source","distance",.1));
        var second=new Document("1","원문",Map.of("sourceId","source","distance",.2));
        assertThat(CoachingRetrievalLab.uniqueDocuments(List.of(first,second))).containsExactly(first);
    }

    @Test void legacyCorpusIsNotPenalizedForEvidenceOnlyAddedInExpandedCorpus() {
        var old=new CoachingRetrievalLab.Need("old","구문");
        var added=new CoachingRetrievalLab.Need("new","새구문");
        assertThat(CoachingRetrievalLab.eligibleNeeds(List.of(old,added),List.of(old))).containsExactly(old);
    }

    @Test void changedGenerationAndExtraOrMissingRowsAreRejectedBeforeWrites() {
        assertThatThrownBy(()->CoachingRetrievalLab.requireGeneration("old","new")).isInstanceOf(IllegalArgumentException.class);
        var doc=new Document("1","원문",Map.of("sourceId","a"));
        var shape=List.of("id:uuid","content:text","metadata:json","embedding:vector(1024)");
        assertThatThrownBy(()->CoachingRetrievalLab.validateReadyGroup(List.of(doc),List.of(),shape)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->CoachingRetrievalLab.validateReadyGroup(List.of(),List.of(doc),shape)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->CoachingRetrievalLab.validateReadyGroup(List.of(doc),List.of(doc),List.of("id:text"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void unsupportedPurposeCropTypeAndStormPredicatesStayConditional() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();
        var spec=mapper.readTree(Path.of("src/test/resources/verification/coaching-retrieval-corpus.json").toFile());
        var cases=mapper.readTree(Path.of("src/test/resources/verification/coaching-retrieval-cases.json").toFile());
        var examples=Map.of("pepper-facility-water","eval-5-watering-0","pepper-facility-setting","eval-5-planting-0",
                "lettuce-leaf-harvest","eval-6-harvest-0","pepper-drainage","eval-5-etc-0");
        for (var entry:examples.entrySet()) {
            JsonNode row=null,input=null;
            for (var r:spec.path("segments")) if(r.path("metadata").path("label").asText().equals(entry.getKey()))row=r;
            for (var c:cases.path("cases")) if(c.path("id").asText().equals(entry.getValue()))input=c;
            var constraints=mapper.convertValue(row.path("constraints"),new TypeReference<Map<String,CoachingEvidenceSelector.Constraint>>() {});
            for(var field:row.path("notStated"))constraints.put(field.asText(),new CoachingEvidenceSelector.Constraint(CoachingEvidenceSelector.State.NOT_STATED,List.of()));
            String body="조건 판정용 합성 본문";
            var rule=new CoachingEvidenceSelector.Applicability(CoachingEvidenceSelector.sha(body),constraints,row.path("unresolvedScope").asBoolean(),List.of("실제 원문의 미지원 조건"));
            var context=mapper.treeToValue(input.path("context"),RecordFeedbackContext.class);
            var facts=mapper.convertValue(input.path("explicitFacts"),new TypeReference<Map<String,CoachingEvidenceSelector.Fact>>() {});
            var result=CoachingEvidenceSelector.select(context,facts,List.of(new Document("fixture",body,Map.of())),Map.of("fixture",rule));
            assertThat(result.getFirst().status()).as(entry.getKey()).isEqualTo(CoachingEvidenceSelector.Status.CONDITIONAL);
        }
    }

    @Test @EnabledIfEnvironmentVariable(named="RUN_COACHING_RETRIEVAL_TRANSACTION_TEST",matches="true")
    void postgresDdlAndRowsAreRolledBackOnInitializationFailure() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(CoachingRetrievalLab.URL,"ccc_coaching_retrieval",System.getenv("POSTGRES_PASSWORD")));
        assertThat(jdbc.queryForObject("select token from lab_identity",String.class)).isEqualTo(System.getenv("LAB_TOKEN"));
        assertThat(jdbc.queryForObject("select to_regclass('public.retrieval_rollback_probe')::text",String.class)).isNull();
        assertThatThrownBy(()->CoachingRetrievalLab.initializeAtomically(jdbc,()->{
            jdbc.execute("create table retrieval_rollback_probe(id integer)");
            jdbc.update("insert into retrieval_rollback_probe values(1)");
            throw new IllegalStateException("simulated embedding failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select to_regclass('public.retrieval_rollback_probe')::text",String.class)).isNull();
    }
}
