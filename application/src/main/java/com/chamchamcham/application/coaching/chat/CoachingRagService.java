package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.coaching.common.*;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CoachingRagService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final CoachingContextProvider contextProvider;
    private final CoachingRetrievalFilterBuilder filterBuilder;
    private final CoachingStructuredOutputValidator validator;
    private final RagProperties ragProperties;
    public CoachingRagService(ChatClient chatClient, VectorStore vectorStore, CoachingContextProvider contextProvider, CoachingRetrievalFilterBuilder filterBuilder, CoachingStructuredOutputValidator validator, RagProperties ragProperties) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient"); this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore"); this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider"); this.filterBuilder = Objects.requireNonNull(filterBuilder, "filterBuilder"); this.validator = Objects.requireNonNull(validator, "validator"); this.ragProperties = Objects.requireNonNull(ragProperties, "ragProperties");
    }
    public CoachingRagResult answer(CoachingRagCommand command) {
        Objects.requireNonNull(command, "command");
        String question = trim(command.getQuestion());
        if (question.isEmpty()) throw new BusinessException(ErrorCode.RAG_INVALID_REQUEST);
        int topK = command.getTopK() == null ? ragProperties.getRetrieval().getTopKDefault() : command.getTopK();
        if (topK < 1 || topK > ragProperties.getRetrieval().getTopKMax()) throw new BusinessException(ErrorCode.RAG_INVALID_REQUEST);
        if (command.getPeriodStart() != null && command.getPeriodEnd() != null && command.getPeriodStart().isAfter(command.getPeriodEnd())) throw new BusinessException(ErrorCode.RAG_INVALID_REQUEST);
        var documents = vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(topK).filterExpression(filterBuilder.build()).build());
        if (documents.isEmpty()) return new CoachingRagResult(CoachingStructuredResult.insufficientEvidence("현재 자료만으로는 판단할 수 없습니다. 기술문서 색인 상태를 확인해주세요."), new RagAuditResult(RagAuditStatus.WARN, Collections.singletonList("no_retrieved_documents"), Collections.emptyList()), modelInfo());
        var context = contextProvider.build(command);
        var advisor = RetrievalAugmentationAdvisor.builder().documentRetriever((DocumentRetriever) ignored -> documents).build();
        CoachingStructuredResult result;
        try { result = chatClient.prompt().system(systemPrompt()).user(userPrompt(question, context)).advisors(advisor).call().entity(CoachingStructuredResult.class); }
        catch (BusinessException exception) { throw exception; }
        catch (RuntimeException exception) { throw new BusinessException(ErrorCode.RAG_STRUCTURED_OUTPUT_INVALID); }
        Set<String> allowedIds = documents.size() == 1 ? Collections.singleton(documents.getFirst().getId()) : new LinkedHashSet<>();
        if (documents.size() != 1) documents.forEach(it -> allowedIds.add(it.getId()));
        return new CoachingRagResult(result, validator.validate(result, allowedIds), modelInfo());
    }
    private String systemPrompt() {
        return "너는 농업 영농 코칭 보조자다.\n반드시 제공된 재배 context와 검색 근거만 사용한다.\n근거가 부족하면 riskLevel은 UNKNOWN, confidence는 0.3 이하로 둔다.\n모든 권장 작업과 다음 행동에는 근거 citationIds를 포함한다.\n응답은 요청된 JSON schema만 따른다.";
    }
    private String userPrompt(String question, CoachingContext context) {
        String text = "            질문:\n            " + question + "\n\n            " + context.getText();
        var lines = Arrays.asList(text.split("\n", -1));
        int indent = lines.stream().filter(it -> !trim(it).isEmpty()).mapToInt(it -> it.length() - trimStart(it).length()).min().orElse(0);
        return String.join("\n", lines.stream().map(it -> it.substring(Math.min(indent, it.length()))).toList());
    }
    private RagModelInfo modelInfo() { return new RagModelInfo(ragProperties.getEmbedding().getModel(), ragProperties.getChat().getModel()); }
    private static String trimStart(String value) { int i = 0; while (i < value.length() && (Character.isWhitespace(value.charAt(i)) || Character.isSpaceChar(value.charAt(i)))) i++; return value.substring(i); }
    private static String trim(String value) { int end = value.length(); while (end > 0 && (Character.isWhitespace(value.charAt(end - 1)) || Character.isSpaceChar(value.charAt(end - 1)))) end--; return trimStart(value.substring(0, end)); }
}
