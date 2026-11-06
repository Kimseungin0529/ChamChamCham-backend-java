package com.chamchamcham.api.rag.client;


import com.chamchamcham.application.coaching.common.RagProperties;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.config.OpenClawProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Primary
public class OpenClawChatModel implements ChatModel {
    private final RestClient restClient;
    private final OpenClawProperties properties;
    private final RagProperties ragProperties;

    public OpenClawChatModel(RestClient.Builder restClientBuilder, OpenClawProperties properties, RagProperties ragProperties) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.ragProperties = Objects.requireNonNull(ragProperties, "ragProperties");
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = restClientBuilder.requestFactory(requestFactory)
                .baseUrl(properties.getBaseUrl().replaceFirst("/+$", "")).build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        if (isBlank(properties.getApiKey()) || isBlank(properties.getAgentId())) {
            throw new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE);
        }
        try {
            var messages = new ArrayList<OpenClawCompletionMessage>(prompt.getInstructions().size());
            for (var instruction : prompt.getInstructions()) messages.add(toOpenClawMessage(instruction));
            var response = restClient.post().uri("/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .header("x-openclaw-agent-id", properties.getAgentId())
                    .body(new OpenClawCompletionRequest(ragProperties.getChat().getModel(), messages))
                    .retrieve().body(OpenClawCompletionResponse.class);
            var choice = response == null || response.getChoices().isEmpty() ? null : response.getChoices().getFirst();
            String content = choice == null || choice.getMessage() == null ? null : choice.getMessage().getContent();
            if (content == null) throw new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException ignored) {
            throw new BusinessException(ErrorCode.RAG_CHAT_UNAVAILABLE);
        }
    }

    private OpenClawCompletionMessage toOpenClawMessage(Message message) {
        return new OpenClawCompletionMessage(message.getMessageType().getValue(), message.getText());
    }

    private boolean isBlank(String value) {
        return value.codePoints().allMatch(character -> Character.isWhitespace(character) || Character.isSpaceChar(character));
    }
}
