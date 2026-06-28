package com.agent.demo6.framework;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class LlmClient {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    private static final int MAX_COMPLETION_TOKENS = 3000;

    private final String apiKey;
    private final HttpClient httpClient;

    public LlmClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AssistantMessage call(List<ChatMessage> messages, String toolsPayloadJson)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(messages, toolsPayloadJson);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "DeepSeek API 请求失败，HTTP status="
                            + response.statusCode()
                            + ", body="
                            + response.body()
            );
        }

        return parseAssistantMessage(response.body());
    }

    private static String buildRequestBody(List<ChatMessage> messages, String toolsPayloadJson) {
        return "{"
                + "\"model\":\"" + JsonUtil.escape(MODEL_NAME) + "\","
                + "\"messages\":" + messagesToJson(messages) + ","
                + "\"tools\":" + toolsPayloadJson + ","
                + "\"tool_choice\":\"auto\","
                + "\"stream\":false,"
                + "\"thinking\":{\"type\":\"disabled\"},"
                + "\"max_tokens\":" + MAX_COMPLETION_TOKENS + ","
                + "\"temperature\":0.2"
                + "}";
    }

    private static String messagesToJson(List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(messageToJson(messages.get(i)));
        }
        builder.append("]");
        return builder.toString();
    }

    private static String messageToJson(ChatMessage message) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"role\":\"").append(JsonUtil.escape(message.role())).append("\"");

        if ("tool".equals(message.role())) {
            builder.append(",\"tool_call_id\":\"")
                    .append(JsonUtil.escape(message.toolCallId()))
                    .append("\"");
            builder.append(",\"content\":\"")
                    .append(JsonUtil.escape(message.content()))
                    .append("\"");
        } else if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            if (message.content() == null) {
                builder.append(",\"content\":null");
            } else {
                builder.append(",\"content\":\"")
                        .append(JsonUtil.escape(message.content()))
                        .append("\"");
            }
            builder.append(",\"tool_calls\":")
                    .append(toolCallsToJson(message.toolCalls()));
        } else {
            builder.append(",\"content\":\"")
                    .append(JsonUtil.escape(message.content()))
                    .append("\"");
        }

        builder.append("}");
        return builder.toString();
    }

    private static String toolCallsToJson(List<ToolCall> toolCalls) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < toolCalls.size(); i++) {
            ToolCall toolCall = toolCalls.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{")
                    .append("\"id\":\"").append(JsonUtil.escape(toolCall.id())).append("\",")
                    .append("\"type\":\"function\",")
                    .append("\"function\":{")
                    .append("\"name\":\"").append(JsonUtil.escape(toolCall.name())).append("\",")
                    .append("\"arguments\":\"").append(JsonUtil.escape(toolCall.argumentsJson())).append("\"")
                    .append("}")
                    .append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private static AssistantMessage parseAssistantMessage(String responseBody) {
        String messageObject = JsonUtil.firstChoiceMessageObject(responseBody);
        String content = JsonUtil.stringField(messageObject, "content");
        String toolCallsArray = JsonUtil.arrayField(messageObject, "tool_calls");

        if (toolCallsArray == null || "[]".equals(toolCallsArray.trim())) {
            return new AssistantMessage(content, List.of());
        }

        List<ToolCall> toolCalls = JsonUtil.splitTopLevelObjects(toolCallsArray).stream()
                .map(toolCallObject -> {
                    String id = JsonUtil.stringField(toolCallObject, "id");
                    String functionObject = JsonUtil.objectField(toolCallObject, "function");
                    String name = JsonUtil.stringField(functionObject, "name");
                    String argumentsJson = JsonUtil.stringField(functionObject, "arguments");
                    if (id == null || name == null || argumentsJson == null) {
                        throw new IllegalStateException("tool_calls 结构不完整：" + toolCallObject);
                    }
                    return new ToolCall(id, name, argumentsJson);
                })
                .toList();

        return new AssistantMessage(content, toolCalls);
    }
}
