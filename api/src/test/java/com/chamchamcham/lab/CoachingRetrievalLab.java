package com.chamchamcham.lab;

import com.chamchamcham.application.coaching.recordfeedback.generation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static com.chamchamcham.lab.CoachingEvidenceSelector.*;

/** 명시적으로 실행하는 실제 검색 비교. ChatModel/제품 SpringContext는 생성하지 않는다. */
public final class CoachingRetrievalLab {
    static final String URL="jdbc:postgresql://127.0.0.1:15434/ccc_coaching_retrieval";
    static final String OLLAMA="http://127.0.0.1:11437";
    static final List<String> GROUPS=List.of("baseline","split","expanded");
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    public record Need(String sourceId,String quote) {}
    public record Measurement(int documents,int required,int recovered,int forbidden) {}

    static void validateTarget(String jdbcUrl,String ollamaUrl) {
        if (!URL.equals(jdbcUrl)||!OLLAMA.equals(ollamaUrl)) throw new IllegalArgumentException("dedicated loopback target required");
    }

    static String databaseMode(String identity,String token,List<String> tables,List<String> schemas) {
        if (token==null||token.isBlank()||!token.equals(identity)||!schemas.equals(List.of("public")))
            throw new IllegalArgumentException("database identity/schema mismatch");
        var actual=new HashSet<>(tables);
        if (actual.equals(Set.of("lab_identity"))) return "EMPTY";
        if (actual.equals(Set.of("lab_identity","baseline","split","expanded"))) return "READY";
        throw new IllegalArgumentException("unexpected or partial database tables");
    }

    static List<Document> uniqueDocuments(List<Document> input) {
        var result=new LinkedHashMap<String,Document>();
        for (var doc:input) {
            var previous=result.putIfAbsent(doc.getId(),doc);
            if (previous!=null&&(!previous.getText().equals(doc.getText())||!storedMetadata(previous).equals(storedMetadata(doc))))
                throw new IllegalArgumentException("conflicting document id");
        }
        return new ArrayList<>(result.values());
    }

    private static Map<String,Object> storedMetadata(Document document) {
        var metadata=new HashMap<>(document.getMetadata());
        // PgVectorStore 1.1.8 adds query-specific distance to each retrieved document.
        metadata.remove("distance");
        return metadata;
    }

    static Measurement measure(List<Document> docs,List<Need> needs,Set<String> forbidden) {
        var unique=new LinkedHashSet<>(needs);
        int recovered=0;
        for (var need:unique) if (docs.stream().anyMatch(doc -> need.sourceId().equals(doc.getMetadata().get("sourceId"))
                &&doc.getText().contains(need.quote()))) recovered++;
        return new Measurement(docs.size(),unique.size(),recovered,(int)docs.stream().filter(doc->forbidden.contains(doc.getId())).count());
    }

    static List<Need> eligibleNeeds(List<Need> applicable,List<Need> declaredForCorpus) {
        var eligible=new HashSet<>(declaredForCorpus);
        return applicable.stream().filter(eligible::contains).distinct().toList();
    }

    static void requireGeneration(String actual,String expected) {
        if (!expected.equals(actual)) throw new IllegalArgumentException("database corpus/rule generation mismatch");
    }

    static void validateReadyGroup(List<Document> expected,List<Document> actual,List<String> columns) {
        if (!columns.equals(List.of("id:uuid","content:text","metadata:json","embedding:vector(1024)")))
            throw new IllegalArgumentException("unexpected vector schema");
        var byId=new HashMap<String,Document>();
        for(var doc:uniqueDocuments(actual))byId.put(doc.getId(),doc);
        if(byId.size()!=expected.size())throw new IllegalArgumentException("missing/extra existing rows; do not repair automatically");
        for(var doc:expected) {
            var stored=byId.get(doc.getId());
            if(stored==null||!doc.getText().equals(stored.getText())||!doc.getMetadata().equals(stored.getMetadata()))
                throw new IllegalArgumentException("existing document mismatch; do not overwrite");
        }
    }

    static void initializeAtomically(JdbcTemplate jdbc,Runnable action) {
        new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(jdbc.getDataSource())))
                .executeWithoutResult(status->action.run());
    }

    static String snapshot(JdbcTemplate jdbc,String table) throws Exception {
        if (!GROUPS.contains(table)) throw new IllegalArgumentException("unknown table");
        var rows=jdbc.queryForList("select id::text,content,metadata::jsonb::text,embedding::text from "+table+" order by id");
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(rows)));
    }

    private static String required(String name) {
        String value=System.getenv(name);
        if (value==null||value.isBlank()) throw new IllegalArgumentException("missing execution setting: "+name);
        return value;
    }

    public static void main(String[] args) throws Exception {
        validateTarget(URL,OLLAMA);
        Path root=Path.of("").toAbsolutePath().normalize(),lab=root.resolve(".local/rag-api/retrieval-expansion");
        String run=required("COACHING_RETRIEVAL_RUN");
        if (!run.matches("[a-z0-9][a-z0-9-]{0,63}")) throw new IllegalArgumentException("invalid run");
        Path output=lab.resolve("runs").resolve(run);
        if (!output.toRealPath().equals(output)||Files.exists(output.resolve("raw.json"))) throw new IllegalArgumentException("run path/overwrite");
        var started=JSON.readTree(output.resolve("started.json").toFile());
        if(!started.path("inputHashes").isObject()||started.path("inputHashes").isEmpty())throw new IllegalArgumentException("missing input hashes");
        for (var iterator=started.path("inputHashes").fields();iterator.hasNext();) {
            var item=iterator.next();Path path=root.resolve(item.getKey()).normalize();
            if (!path.startsWith(root)||!path.toRealPath().startsWith(root)||!fileSha(path).equals(item.getValue().asText()))
                throw new IllegalArgumentException("changed input");
        }
        var corpus=JSON.readTree(lab.resolve("prepared.json").toFile());
        var cases=JSON.readTree(root.resolve("api/src/test/resources/verification/coaching-retrieval-cases.json").toFile()).path("cases");
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(URL,"ccc_coaching_retrieval",required("POSTGRES_PASSWORD")));
        String mode=databaseMode(jdbc.queryForObject("select token from lab_identity",String.class),required("LAB_TOKEN"),
                jdbc.queryForList("select c.relname from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relkind in ('r','p','v','m','S','f') order by 1",String.class),
                jdbc.queryForList("select nspname from pg_namespace where nspname !~ '^pg_' and nspname<>'information_schema' order by 1",String.class));
        requireGeneration(jdbc.queryForObject("select corpus_sha256 from lab_identity",String.class),fileSha(lab.resolve("prepared.json")));
        if(!mode.equals(jdbc.queryForObject("select load_state from lab_identity",String.class)))throw new IllegalArgumentException("inconsistent database load state");
        // All groups and their stored rows are checked before any embedding call or write.
        var expected=new LinkedHashMap<String,List<Document>>();
        var rules=new LinkedHashMap<String,Applicability>();
        for(var group:GROUPS) {
            var docs=new ArrayList<Document>();
            for(var row:corpus.path(group)) {
                String id=row.path("id").asText();UUID.fromString(id);String text=row.path("text").asText();
                var metadata=JSON.convertValue(row.path("metadata"),new TypeReference<Map<String,Object>>() {});
                var rule=JSON.treeToValue(row.path("rule"),Applicability.class);
                if(!sha(text).equals(rule.textSha256())||!rule.constraints().keySet().equals(FIELDS))throw new IllegalArgumentException("prepared rule binding");
                var previous=rules.putIfAbsent(id,rule);
                if(previous!=null&&!previous.equals(rule))throw new IllegalArgumentException("conflicting rule");
                docs.add(new Document(id,text,metadata));
            }
            if(uniqueDocuments(docs).size()!=docs.size()||docs.isEmpty())throw new IllegalArgumentException("invalid corpus");
            expected.put(group,docs);
            if(mode.equals("READY")) {
                var columns=jdbc.queryForList("select attname||':'||format_type(atttypid,atttypmod) from pg_attribute where attrelid='public."
                        +group+"'::regclass and attnum>0 and not attisdropped order by attnum",String.class);
                var stored=new ArrayList<Document>();
                for(var row:jdbc.queryForList("select id::text,content,metadata::text as metadata from "+group))
                    stored.add(new Document(row.get("id").toString(),row.get("content").toString(),JSON.readValue(row.get("metadata").toString(),new TypeReference<Map<String,Object>>() {})));
                validateReadyGroup(docs,stored,columns);
                if(jdbc.queryForObject("select count(*) from "+group+" where embedding is null or vector_dims(embedding)<>1024",Integer.class)!=0)
                    throw new IllegalArgumentException("invalid stored vector");
            }
        }
        var client=java.net.http.HttpClient.newHttpClient();
        var tags=JSON.readTree(client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(OLLAMA+"/api/tags")).timeout(java.time.Duration.ofSeconds(15)).build(),java.net.http.HttpResponse.BodyHandlers.ofString()).body());
        boolean matching=false;
        for(var model:tags.path("models"))if(model.path("name").asText().equals("bge-m3:latest")&&model.path("digest").asText().equals("7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab"))matching=true;
        if(!matching)throw new IllegalArgumentException("embedding identity mismatch");
        var loaded=JSON.readTree(client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(OLLAMA+"/api/ps")).timeout(java.time.Duration.ofSeconds(15)).build(),java.net.http.HttpResponse.BodyHandlers.ofString()).body());
        for(var model:loaded.path("models"))if(!model.path("name").asText().equals("bge-m3:latest"))throw new IllegalArgumentException("another model is loaded");
        var embedding=OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder().baseUrl(OLLAMA).build())
                .defaultOptions(OllamaEmbeddingOptions.builder().model("bge-m3").truncate(false).keepAlive("5m").build()).build();
        var stores=new LinkedHashMap<String,PgVectorStore>();
        var database=new LinkedHashMap<String,Object>();
        for (var group:GROUPS) {
            var store=PgVectorStore.builder(jdbc,embedding).vectorTableName(group).dimensions(1024)
                    .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE).indexType(PgVectorStore.PgIndexType.HNSW)
                    .initializeSchema(mode.equals("EMPTY")).maxDocumentBatchSize(1).build();
            stores.put(group,store);
        }
        if(mode.equals("EMPTY"))initializeAtomically(jdbc,()->{
            for(var group:GROUPS) {
                stores.get(group).afterPropertiesSet();
                for(var doc:expected.get(group)) {
                    stores.get(group).add(List.of(doc));
                    int matchingRows=jdbc.queryForObject("select count(*) from "+group+" where id=?::uuid and content=? and metadata::jsonb=?::jsonb and vector_dims(embedding)=1024",
                            Integer.class,doc.getId(),doc.getText(),JSON.valueToTree(doc.getMetadata()).toString());
                    if(matchingRows!=1)throw new IllegalArgumentException("inserted document mismatch");
                }
            }
            jdbc.update("update lab_identity set load_state='READY'");
        });
        else stores.values().forEach(PgVectorStore::afterPropertiesSet);
        for(var group:GROUPS) {
            int count=jdbc.queryForObject("select count(*) from "+group,Integer.class);
            if (count!=corpus.path(group).size()) throw new IllegalArgumentException("unexpected existing rows");
            int added=mode.equals("EMPTY")?count:0;
            database.put(group,Map.of("added",added,"count",count,"snapshotSha256",snapshot(jdbc,group)));
            System.out.println("Prepared "+group+": "+count+", added "+added);
        }
        var rows=new ArrayList<Map<String,Object>>();var totals=new LinkedHashMap<String,Map<String,Integer>>();
        var planner=new RecordFeedbackRetrievalQueryPlanner();
        String split=required("COACHING_RETRIEVAL_SPLIT");
        if (!Set.of("dev","regression").contains(split)) throw new IllegalArgumentException("invalid split");
        String sourceSplit=split.equals("regression")?"evaluation":split;
        var planned=JSON.convertValue(started.path("plannedCaseIds"),new TypeReference<Set<String>>() {});
        if(planned==null||planned.size()!=(split.equals("dev")?24:96))throw new IllegalArgumentException("incomplete planned cases");
        Set<String> ids=new HashSet<>();
        for (var input:cases) {
            if (!input.path("split").asText().equals(sourceSplit)) continue;
            String id=input.path("id").asText();
            if (!ids.add(id)||!input.path("synthetic").asBoolean()) throw new IllegalArgumentException("invalid case");
            var context=JSON.treeToValue(input.path("context"),RecordFeedbackContext.class);
            var facts=JSON.convertValue(input.path("explicitFacts"),new TypeReference<Map<String,Fact>>() {});
            List<Need> needs=JSON.convertValue(input.path("requiredEvidence"),new TypeReference<List<Need>>() {});
            var queries=planner.plan(context);
            for (var group:GROUPS) {
                if (!group.equals("expanded")&&!Set.of("천궁","참당귀","구기자나무").contains(context.crop().name())) continue;
                var fetched=new ArrayList<Document>();var trace=new ArrayList<Object>();long start=System.nanoTime();
                for (var query:queries) {
                    var request=SearchRequest.builder().query(query.query()).topK(6).similarityThreshold(.5)
                            .filterExpression("sourceType == 'TECH_DOCUMENT' && cropName in ['"+context.crop().name().replace("'","")+"', 'GENERAL']").build();
                    var found=stores.get(group).similaritySearch(request);fetched.addAll(found);
                    trace.add(Map.of("query",query.query(),"threshold",.5,"topK",6,"documents",found.stream().map(CoachingRetrievalLab::documentRow).toList()));
                }
                var unique=uniqueDocuments(fetched);double elapsed=(System.nanoTime()-start)/1_000_000.0;
                var forbidden=JSON.convertValue(input.path("forbiddenIds").path(group),new TypeReference<Set<String>>() {});
                // 기대값은 검색/조건선별 입력에 포함하지 않는다.
                List<Need> groupNeeds=group.equals("expanded")?needs:JSON.convertValue(input.path("legacyRequiredEvidence"),new TypeReference<List<Need>>() {});
                addRow(rows,totals,id,input,group,unique,groupNeeds,forbidden,List.of(),trace,elapsed);
                if (group.equals("expanded")) {
                    var decisions=select(context,facts,unique,rules);
                    var allowed=new HashSet<String>();decisions.stream().filter(d->d.status()==Status.APPLICABLE).forEach(d->allowed.add(d.documentId()));
                    addRow(rows,totals,id,input,"selected",unique.stream().filter(d->allowed.contains(d.getId())).toList(),needs,forbidden,decisions,trace,elapsed);
                }
            }
            System.out.println("Completed "+id);
            Files.writeString(output.resolve("progress.jsonl"),JSON.writeValueAsString(Map.of("caseId",id))+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        }
        for (var group:GROUPS) {
            @SuppressWarnings("unchecked") var original=(Map<String,Object>)database.get(group);
            if (!original.get("snapshotSha256").equals(snapshot(jdbc,group))) throw new IllegalStateException("database changed during retrieval");
        }
        if(!ids.equals(planned))throw new IllegalArgumentException("incomplete/extra executed cases");
        var summary=new LinkedHashMap<String,Object>();summary.put("split",split);summary.put("independentEvaluation",false);summary.put("cases",ids.size());summary.put("arms",totals);
        summary.put("chatRequests",0);summary.put("automaticMemoExtraction",false);summary.put("answerAccuracyEvaluated",false);
        var result=Map.of("summary",summary,"database",database,"cases",rows,"model","bge-m3","dimension",1024);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve("raw.json").toFile(),result);
        System.out.println(JSON.writeValueAsString(summary));
    }

    private static Map<String,Object> documentRow(Document doc) {
        var row=new LinkedHashMap<String,Object>();row.put("id",doc.getId());row.put("text",doc.getText());row.put("metadata",doc.getMetadata());row.put("score",doc.getScore());return row;
    }

    private static void addRow(List<Map<String,Object>> rows,Map<String,Map<String,Integer>> totals,String id,JsonNode input,
            String arm,List<Document> docs,List<Need> needs,Set<String> forbidden,List<Decision> decisions,List<Object> trace,double elapsed) {
        var measurement=measure(docs,needs,forbidden);
        List<Need> applicable=JSON.convertValue(input.path("expectedApplicableEvidence"),new TypeReference<List<Need>>() {});
        // 개발 자료에는 이 사전 주석이 없다. 새 평가의 유용한 근거 손실과 조건부 문서 제외를 구분한다.
        if (applicable==null) applicable=List.of();
        var applicableMeasurement=measure(docs,eligibleNeeds(applicable,needs),Set.of());
        var total=totals.computeIfAbsent(arm,key->new LinkedHashMap<>());
        total.merge("cases",1,Integer::sum);total.merge("documents",measurement.documents(),Integer::sum);
        total.merge("required",measurement.required(),Integer::sum);total.merge("recovered",measurement.recovered(),Integer::sum);
        total.merge("forbidden",measurement.forbidden(),Integer::sum);total.merge("emptyCases",docs.isEmpty()?1:0,Integer::sum);
        total.merge("applicableRequired",applicableMeasurement.required(),Integer::sum);
        total.merge("applicableRecovered",applicableMeasurement.recovered(),Integer::sum);
        total.merge("casesWithRequired",needs.isEmpty()?0:1,Integer::sum);
        total.merge("allRequiredRecoveredCases",!needs.isEmpty()&&measurement.required()==measurement.recovered()?1:0,Integer::sum);
        var row=new LinkedHashMap<String,Object>();row.put("caseId",id);row.put("split",input.path("split").asText());row.put("arm",arm);
        row.put("crop",input.path("context").path("crop").path("name").asText());row.put("workType",input.path("context").path("record").path("workType").asText());
        row.put("queries",trace);row.put("documents",docs.stream().map(CoachingRetrievalLab::documentRow).toList());row.put("decisions",decisions);
        row.put("measurement",measurement);row.put("applicableMeasurement",applicableMeasurement);row.put("elapsedMs",elapsed);row.put("gaps",input.path("gaps"));rows.add(row);
    }

    private static String fileSha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
