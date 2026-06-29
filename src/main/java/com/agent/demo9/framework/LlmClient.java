package com.agent.demo9.framework;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

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

    public String askText(String systemPrompt, String userContent) throws IOException, InterruptedException {
        String requestBody = buildRequestBody(systemPrompt, userContent);

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

        String messageObject = JsonUtil.firstChoiceMessageObject(response.body());
        String content = JsonUtil.stringField(messageObject, "content");
        return content == null ? "" : content;
    }

    public Map<String, String> askJson(String systemPrompt, String userContent)
            throws IOException, InterruptedException {
        String text = stripCodeFence(askText(systemPrompt, userContent).trim());
        if (!text.startsWith("{")) {
            throw new IllegalStateException("模型没有返回 JSON 对象：" + text);
        }
        return JsonUtil.flatStringMap(text);
    }

    private static String buildRequestBody(String systemPrompt, String userContent) {
        return "{"
                + "\"model\":\"" + JsonUtil.escape(MODEL_NAME) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + JsonUtil.escape(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + JsonUtil.escape(userContent) + "\"}"
                + "],"
                + "\"stream\":false,"
                + "\"thinking\":{\"type\":\"disabled\"},"
                + "\"max_tokens\":" + MAX_COMPLETION_TOKENS + ","
                + "\"temperature\":0.2"
                + "}";
    }

    private static String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }

        String[] lines = text.split("\\R");
        if (lines.length < 3) {
            return text;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < lines.length - 1; i++) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(lines[i]);
        }
        return builder.toString().trim();
    }
}
