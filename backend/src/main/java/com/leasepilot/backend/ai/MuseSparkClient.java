package com.leasepilot.backend.ai;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Talks to Muse Spark through the Meta Model API's chat completions endpoint.
// Docs: https://dev.meta.ai/docs/protocols/chat-completions
@Component
public class MuseSparkClient {

    private final RestClient restClient;
    private final String model;

    public MuseSparkClient(RestClient metaApiRestClient,
                           @Value("${meta.api.chat-model}") String model) {
        this.restClient = metaApiRestClient;
        this.model = model;
    }

    // Sends one user message and returns Muse Spark's text reply
    public String chat(String userMessage) {
        return complete(List.of(Message.user(userMessage)), null, null);
    }

    // Any messages (text and/or images), optional JSON schema for the answer,
    // optional reasoning effort ("minimal", "low", "medium", "high", "xhigh", "max").
    // Returns the reply text; with a schema, that text is JSON matching the schema.
    public String complete(List<Message> messages, Map<String, Object> responseFormat, String reasoningEffort) {
        return send(messages, null, responseFormat, reasoningEffort).content();
    }

    // Lowest level: returns the whole assistant message, which may contain tool calls.
    // Docs: https://dev.meta.ai/docs/tool-calling
    public AssistantMessage send(List<Message> messages, List<Map<String, Object>> tools,
                                 Map<String, Object> responseFormat, String reasoningEffort) {
        ChatRequest request = new ChatRequest(model, messages, tools, responseFormat, reasoningEffort);

        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Muse Spark returned no choices");
        }
        return response.choices().get(0).message();
    }

    public String getModel() {
        return model;
    }

    // Builds the response_format that makes Muse Spark answer with JSON matching the schema.
    // Docs: https://dev.meta.ai/docs/structured-output
    public static Map<String, Object> jsonSchemaFormat(String name, Map<String, Object> schema) {
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of("name", name, "strict", true, "schema", schema));
    }

    // Describes one tool (function) the model may call. parameters is a JSON schema.
    public static Map<String, Object> functionTool(String name, String description, Map<String, Object> parameters) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", parameters,
                        "strict", true));
    }

    // --- JSON shapes for the request and response ---

    // content is a plain String or a List<ContentPart> (text + images).
    // Assistant messages may carry toolCalls; tool-result messages carry toolCallId.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Message(
            String role,
            Object content,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId) {

        public static Message system(String text) {
            return new Message("system", text, null, null);
        }

        public static Message user(String text) {
            return new Message("user", text, null, null);
        }

        public static Message user(List<ContentPart> parts) {
            return new Message("user", parts, null, null);
        }

        // Echo the model's own message back into the conversation (required before tool results)
        public static Message assistant(AssistantMessage reply) {
            return new Message("assistant", reply.content(), reply.toolCalls(), null);
        }

        public static Message toolResult(String toolCallId, String resultJson) {
            return new Message("tool", resultJson, null, toolCallId);
        }
    }

    // One piece of a multi-part message: either text or an image.
    // Docs: https://dev.meta.ai/docs/image-understanding
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ContentPart(String type, String text, @JsonProperty("image_url") ImageUrl imageUrl) {

        public static ContentPart text(String text) {
            return new ContentPart("text", text, null);
        }

        // Sends the image inline as a base64 data URL, e.g. "data:image/jpeg;base64,..."
        public static ContentPart image(byte[] bytes, String mimeType) {
            String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            return new ContentPart("image_url", null, new ImageUrl(dataUrl));
        }
    }

    public record ImageUrl(String url) {
    }

    // A tool the model wants us to run. arguments is a JSON string, e.g. "{\"category\":\"PLUMBING\"}"
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(String id, String type, FunctionCall function) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(String name, String arguments) {
    }

    // The model's reply: text, tool calls, or both
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantMessage(String content, @JsonProperty("tool_calls") List<ToolCall> toolCalls) {

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(
            String model,
            List<Message> messages,
            List<Map<String, Object>> tools,
            @JsonProperty("response_format") Map<String, Object> responseFormat,
            @JsonProperty("reasoning_effort") String reasoningEffort) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(AssistantMessage message, @JsonProperty("finish_reason") String finishReason) {
    }
}
