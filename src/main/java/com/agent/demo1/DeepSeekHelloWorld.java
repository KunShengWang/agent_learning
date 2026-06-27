package com.agent.demo1;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class DeepSeekHelloWorld {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        // 构建聊天消息
        List<ChatMessage> messages = buildMessages();

        // 只是发送给 LLM 的 messages 部分
        System.out.println("=== 发送给模型的消息 ===");
        System.out.println(toPrettyMessagesJson(messages));

        // 调用 LLM，向其发送消息
        String responseBody = callLlm(apiKey, messages);
        System.out.println("\n=== responseBody ===");
        System.out.println("responseBody : " + responseBody);
        // 提取首选内容
        String answer = extractFirstChoiceContent(responseBody);

        System.out.println("\n=== 模型回复 ===");
        System.out.println(answer);

        String usage = extractObjectField(responseBody, "usage");
        if (usage != null) {
            System.out.println("\n=== Token 用量 ===");
            System.out.println(usage);
        }
    }

    /**
     * 构建聊天消息
     */
    private static List<ChatMessage> buildMessages() {
        return List.of(
                new ChatMessage(
                        "system",
                        "你是一个面向初学者的 Java 和 agent 助手。"
                                + "回答时尽量简洁、友好，并在必要时给出清晰步骤。"
                ),
                new ChatMessage(
                        "user",
                        "请用一句话介绍什么是 Agent，并给一个生活中的类比。"
                )
        );
    }

    /**
     * 调用 LLM，向其发送消息
     */
    private static String callLlm(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 构建 LLM 请求体
        String requestBody = buildRequestBody(messages);
        System.out.println("\n=== requestBody ===");
        System.out.println("requestBody : " + requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "DeepSeek API 请求失败，HTTP status="
                            + response.statusCode()
                            + ", body="
                            + response.body()
            );
        }

        return response.body();
    }

    /**
     * 构建 LLM 请求体
     */
    private static String buildRequestBody(List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"model\":\"").append(jsonEscape(MODEL_NAME)).append("\",");
        builder.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{")
                    .append("\"role\":\"").append(jsonEscape(message.role())).append("\",")
                    .append("\"content\":\"").append(jsonEscape(message.content())).append("\"")
                    .append("}");
        }
        builder.append("],");
        builder.append("\"stream\":false,");
        builder.append("\"thinking\":{\"type\":\"disabled\"},");
        builder.append("\"max_tokens\":200,");
        builder.append("\"temperature\":0.7");
        builder.append("}");
        return builder.toString();
    }

    /**
     * 转换为格式化 JSON 消息
     */
    private static String toPrettyMessagesJson(List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            builder.append("  {\n")
                    .append("    \"role\": \"").append(jsonEscape(message.role())).append("\",\n")
                    .append("    \"content\": \"").append(jsonEscape(message.content())).append("\"\n")
                    .append("  }");
            if (i < messages.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 提取首选内容
     */
    private static String extractFirstChoiceContent(String json) {
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) {
            throw new IllegalStateException("响应中没有 choices 字段：" + json);
        }

        int contentKeyIndex = json.indexOf("\"content\"", choicesIndex);
        if (contentKeyIndex < 0) {
            throw new IllegalStateException("响应中没有 choices[0].message.content 字段：" + json);
        }

        int colonIndex = json.indexOf(':', contentKeyIndex);
        int firstQuoteIndex = json.indexOf('"', colonIndex + 1);
        if (colonIndex < 0 || firstQuoteIndex < 0) {
            throw new IllegalStateException("content 字段格式不正确：" + json);
        }

        return readJsonString(json, firstQuoteIndex);
    }

    private static String extractObjectField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int objectStart = json.indexOf('{', colonIndex + 1);
        if (colonIndex < 0 || objectStart < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = objectStart; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(objectStart, i + 1);
                }
            }
        }

        return null;
    }

    private static String readJsonString(String json, int openingQuoteIndex) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = openingQuoteIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                builder.append(switch (current) {
                    case '"' -> '"';
                    case '\\' -> '\\';
                    case '/' -> '/';
                    case 'b' -> '\b';
                    case 'f' -> '\f';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> current;
                });
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            if (current == '"') {
                return builder.toString();
            }

            builder.append(current);
        }

        throw new IllegalStateException("JSON 字符串没有正常结束：" + json);
    }

    /**
     * JSON 转义
     */
    private static String jsonEscape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }

    public record ChatMessage(String role, String content) {
    }
}
