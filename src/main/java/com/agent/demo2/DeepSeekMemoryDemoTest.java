package com.agent.demo2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeepSeekMemoryDemoTest {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    private static final int MAX_TURNS = 4;

    public static void main(String[] args) throws IOException {
        // 读取 LLM 的 API_LEY
        String apiKey = getApiKey();

        MessageStore messageStore = new MessageStore(MAX_TURNS);
        // 添加系统消息
        messageStore.append(createSystemMessage());

        System.out.println("Memory Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("当前会保留最近 " + MAX_TURNS + " 轮对话作为短期记忆。");
        System.out.println("你可以试试：先说“我叫小明”，再问“我叫什么？”");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true){
            System.out.println("\n你：");
            String userInput = reader.readLine();
            if (userInput == null) {
                break;
            }

            userInput = userInput.trim();
            if (userInput.isEmpty()) {
                System.out.println("请输入内容。");
                continue;
            }

            if ("exit".equalsIgnoreCase(userInput) || "quit".equalsIgnoreCase(userInput)) {
                System.out.println("对话结束。");
                break;
            }
            // 往会话记忆中添加 user 信息
            messageStore.append(new ChatMessage("user", userInput));

            try{
                String answer = callLlm(apiKey, messageStore.snapshot());
                System.out.println("\n助手：" + answer);

                messageStore.append(new ChatMessage("assistant", answer));
                System.out.println("[调试] 当前记忆消息数：" + messageStore.snapshot().size());
            }catch (IOException | InterruptedException | RuntimeException ex) {
                System.out.println("\n请求失败：" + ex.getMessage());
                messageStore.removeLastIfRole("user");
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 获取系统变量中的 api_key
     */
    private static String getApiKey(){
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if(apiKey == null || apiKey.isBlank()){
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }
        return apiKey;
    }

    private static ChatMessage createSystemMessage(){
        return new ChatMessage(
                "system",
                "你是一个面向初学者的 Java 和 agent 助手。"
                        + "请使用简洁、友好、清晰的中文回答。"
                        + "如果用户的问题依赖上文，请结合对话历史继续回答。"
        );
    }

    private static String callLlm(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String requestBody = buildRequestBody(messages);
        System.out.println("[调试] requestBody = " + requestBody);
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

        return extractFirstChoiceContent(response.body());
    }

    private static String buildRequestBody(List<ChatMessage> messages){
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"model\": \"").append(MODEL_NAME).append("\",\n");
        builder.append("  \"messages\": [\n");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage chatMessage = messages.get(i);
            builder.append("    {\n")
                    .append("      \"role\": \"").append(jsonEscape(chatMessage.role())).append("\",\n")
                    .append("      \"content\": \"").append(jsonEscape(chatMessage.content())).append("\"\n")
                    .append("    }");
            if(i < messages.size() - 1){
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ],\n");
        builder.append("  \"stream\": false,\n");
        builder.append("  \"thinking\": {\n").append("    \"type\": ").append("\"disabled\"\n").append("  },\n");
        builder.append("  \"max_tokens\": 200,\n");
        builder.append("  \"temperature\": 0.7\n");
        builder.append("}");
        return builder.toString();
    }

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

    private static String readJsonString(String json, int openingQuoteIndex) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int i = openingQuoteIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaping) {
                if (current == 'u' && i + 4 < json.length()) {
                    String hex = json.substring(i + 1, i + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                } else {
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
                }
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

    public static class MessageStore{

        private final int maxTurns;
        private final List<ChatMessage> messages = new ArrayList<>();

        public MessageStore(int maxTurns){
            this.maxTurns = maxTurns;
        }

        public void append(ChatMessage chatMessage){
            messages.add(chatMessage);
            trim();
        }

        private void trim(){
            if(messages.isEmpty()){
                return;
            }
            // 取出系统提示词
            ChatMessage chatMessage = messages.get(0);
            List<ChatMessage> recentMessages = new ArrayList<>(messages.subList(1, messages.size()));
            int maxMessageCount = maxTurns * 2;
            // 会话长度缩减
            if(recentMessages.size() > maxMessageCount){
                recentMessages = recentMessages.subList(
                        recentMessages.size() - maxMessageCount,
                        recentMessages.size()
                );
            }
            messages.clear();
            messages.add(chatMessage);
            messages.addAll(recentMessages);
        }

        public List<ChatMessage> snapshot(){
            return List.copyOf(messages);
        }

        /**
         * 取出最后一个 role 消息
         */
        public void removeLastIfRole(String role){
            if(!messages.isEmpty() && Objects.equals(role,messages.get(messages.size() - 1).role())){
                messages.remove(messages.size() - 1);
            }
        }
    }

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

    private record ChatMessage(String role, String content){}
}
