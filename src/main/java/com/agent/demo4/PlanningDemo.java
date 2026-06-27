package com.agent.demo4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PlanningDemo {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    // 最大代理步骤数
    private static final int MAX_AGENT_STEPS = 6;
    // 最大完成 tokens
    private static final int MAX_COMPLETION_TOKENS = 2500;
    // 最大会话历史轮次
    private static final int MAX_HISTORY_TURNS = 6;
    // 生成文件目录
    private static final Path GENERATED_FILES_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo4")
            .toAbsolutePath()
            .normalize();

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        System.out.println("Planning Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我生成一份 Java Agent 学习计划，并保存成 markdown 文件。");
        System.out.println("生成的文件会保存到：" + GENERATED_FILES_DIR);
        System.out.println("当前会保留最近 " + MAX_HISTORY_TURNS + " 轮会话记忆。");

        List<ChatMessage> history = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("\n你：");
            String userGoal = reader.readLine();
            if (userGoal == null) {
                break;
            }

            userGoal = userGoal.trim();
            if (userGoal.isEmpty()) {
                System.out.println("请输入任务目标。");
                continue;
            }

            if ("exit".equalsIgnoreCase(userGoal) || "quit".equalsIgnoreCase(userGoal)) {
                System.out.println("对话结束。");
                break;
            }

            history.add(ChatMessage.user(userGoal));
            history = trimMessages(history, MAX_HISTORY_TURNS);

            try {
                String finalAnswer = runTaskAgent(apiKey, userGoal, history);
                history.add(ChatMessage.assistant(finalAnswer));
                history = trimMessages(history, MAX_HISTORY_TURNS);
                System.out.println("\n助手：" + finalAnswer);
            } catch (IOException | InterruptedException | RuntimeException ex) {
                System.out.println("\n执行失败：" + ex.getMessage());
                removeLastUserMessage(history);
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static String runTaskAgent(String apiKey, String userGoal, List<ChatMessage> history)
            throws IOException, InterruptedException {
        TaskState state = new TaskState();
        List<String> stepLogs = new ArrayList<>();

        for (int stepNumber = 1; stepNumber <= MAX_AGENT_STEPS; stepNumber++) {
            System.out.println("\n[调试] stepLogs = " + stepLogs);

            PlanningDecision decision = callPlanner(apiKey, userGoal, history, state, stepLogs);
            System.out.println("\n[调试] decision = " + decision);

            // 要执行的动作之一：decide_path、draft_content、create_file、finish
            String action = decision.action();

            // 要执行的步骤说明
            String stepSummary = decision.stepSummary();
            if (stepSummary == null || stepSummary.isBlank()) {
                stepSummary = "模型没有提供步骤说明。";
            }

            System.out.println("\n[步骤 " + stepNumber + "] " + action);
            System.out.println("[步骤说明] " + stepSummary);

            applyModelUpdates(state, decision);

            if ("decide_path".equals(action)) {
                if (state.relativePath == null || state.relativePath.isBlank()) {
                    throw new IllegalStateException("模型选择了 decide_path，但没有提供 relative_path。");
                }
                stepLogs.add("已确定文件路径：" + state.relativePath);
                continue;
            }

            if ("draft_content".equals(action)) {
                if (state.content == null || state.content.isBlank()) {
                    throw new IllegalStateException("模型选择了 draft_content，但没有提供 content。");
                }
                stepLogs.add("已生成文件内容草稿，长度约 " + state.content.length() + " 个字符。");
                continue;
            }

            if ("create_file".equals(action)) {
                if (state.relativePath == null || state.relativePath.isBlank()) {
                    throw new IllegalStateException("模型选择了 create_file，但还没有 relative_path。");
                }
                if (state.content == null || state.content.isBlank()) {
                    throw new IllegalStateException("模型选择了 create_file，但还没有 content。");
                }

                ToolResult toolResult = createTextFile(state.relativePath, state.content, state.overwrite);
                System.out.println("\n[调试] toolResult = " + toolResult);

                state.toolResultJson = toolResult.toJson();
                state.fileCreated = toolResult.ok();

                System.out.println("[工具结果] " + state.toolResultJson);

                if (toolResult.ok()) {
                    stepLogs.add("已创建文件：" + toolResult.path());
                } else {
                    stepLogs.add("创建文件失败：" + toolResult.error());
                }
                continue;
            }

            if ("finish".equals(action)) {
                String finalResponse = decision.finalResponse();
                if (finalResponse == null || finalResponse.isBlank()) {
                    if (state.fileCreated && state.toolResultJson != null) {
                        finalResponse = "任务已完成，文件已创建：" + state.toolResultJson;
                    } else {
                        finalResponse = "任务已结束。";
                    }
                }
                return finalResponse;
            }

            throw new IllegalStateException("模型返回了未知动作：" + action);
        }

        throw new IllegalStateException("超过最大步骤数，任务仍未完成。可以把任务描述得更具体一点再重试。");
    }

    private static PlanningDecision callPlanner(
            String apiKey,
            String userGoal,
            List<ChatMessage> history,
            TaskState state,
            List<String> stepLogs
    ) throws IOException, InterruptedException {
        List<ChatMessage> requestMessages = new ArrayList<>();
        requestMessages.add(createSystemMessage());
        requestMessages.addAll(trimMessages(history, MAX_HISTORY_TURNS));
        requestMessages.add(buildStateMessage(userGoal, state, stepLogs));
        System.out.println("\n[调试] requestMessages = " + requestMessages);

        String responseBody = callLlm(apiKey, requestMessages);
        System.out.println("\n[调试] responseBody =" + responseBody);

        String rawContent = extractFirstChoiceContent(responseBody);
        System.out.println("\n[调试] rawContent =" + rawContent);

        // 解析 json 字符串为 PlanningDecision 对象
        return parsePlanningDecision(rawContent);
    }

    private static ChatMessage createSystemMessage() {
        return ChatMessage.system(
                "你是一个分步执行任务的 Agent。"
                        + "你不能一次性跳过所有步骤，而是要根据当前状态决定下一步动作。"
                        + "你必须结合已有的会话历史理解用户偏好、默认约定和上文提到的内容。"
                        + "你只允许返回 JSON，不要输出 markdown，不要输出额外解释。"
                        + "可选动作只有四种：decide_path、draft_content、create_file、finish。"
                        + "动作规则如下："
                        + "1. 如果还没有文件路径，优先 decide_path。"
                        + "2. 如果还没有文件内容，优先 draft_content。"
                        + "3. 如果路径和内容都准备好了，但文件还没创建，使用 create_file。"
                        + "4. 只有在任务已经完成，或者不需要再执行动作时，才能 finish。"
                        + "返回 JSON 时请使用字段：step_summary、action、relative_path、content、final_response。"
                        + "其中 step_summary 是简短的人类可读步骤说明，不要暴露冗长推理。"
                        + "relative_path 和 content 可以为 null，但在需要时必须提供完整值。"
                        + "所有待创建文件都必须位于 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo4 目录之下。"
        );
    }

    private static ChatMessage buildStateMessage(String userGoal, TaskState state, List<String> stepLogs) {
        StringBuilder logsBuilder = new StringBuilder();
        if (stepLogs.isEmpty()) {
            logsBuilder.append("- 暂无");
        } else {
            for (String log : stepLogs) {
                logsBuilder.append("- ").append(log).append("\n");
            }
        }

        String content = """
                用户目标：
                %s

                当前状态：
                - relative_path: %s
                - has_content: %s
                - file_created: %s
                - overwrite: %s
                - tool_result:
                %s

                之前已经执行的步骤：
                %s

                请只返回一个 JSON 对象，字段如下：
                {
                  "step_summary": "简短说明下一步要做什么",
                  "action": "decide_path | draft_content | create_file | finish",
                  "relative_path": "字符串或 null",
                  "content": "字符串或 null",
                  "final_response": "字符串或 null"
                }
                """.formatted(
                userGoal,
                state.relativePath,
                state.content == null || state.content.isBlank() ? "no" : "yes",
                state.fileCreated,
                state.overwrite,
                state.toolResultJson == null ? "null" : state.toolResultJson,
                logsBuilder.toString().trim()
        );

        return ChatMessage.user(content);
    }

    private static String callLlm(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String requestBody = buildRequestBody(messages);
        System.out.println("\n[调试] requestBody =" + requestBody);

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

        return response.body();
    }

    private static String buildRequestBody(List<ChatMessage> messages) {
        return "{"
                + "\"model\":\"" + jsonEscape(MODEL_NAME) + "\","
                + "\"messages\":" + messagesToJson(messages) + ","
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
            ChatMessage message = messages.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append("{")
                    .append("\"role\":\"").append(jsonEscape(message.role())).append("\",")
                    .append("\"content\":\"").append(jsonEscape(message.content())).append("\"")
                    .append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * 解析 json 字符串为 PlanningDecision 对象
     */
    private static PlanningDecision parsePlanningDecision(String rawContent) {
        String text = stripMarkdownFence(rawContent).trim();
        if (!text.startsWith("{")) {
            int objectStart = text.indexOf('{');
            int objectEnd = text.lastIndexOf('}');
            if (objectStart >= 0 && objectEnd > objectStart) {
                text = text.substring(objectStart, objectEnd + 1);
            }
        }

        String action = extractStringField(text, "action");
        if (action == null || action.isBlank()) {
            throw new IllegalStateException("模型没有返回合法 action，原始内容：" + rawContent);
        }

        return new PlanningDecision(
                extractStringField(text, "step_summary"),
                action,
                extractStringField(text, "relative_path"),
                extractStringField(text, "content"),
                extractStringField(text, "final_response")
        );
    }

    private static String stripMarkdownFence(String rawContent) {
        String text = rawContent.trim();
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
                builder.append('\n');
            }
            builder.append(lines[i]);
        }
        return builder.toString();
    }

    private static void applyModelUpdates(TaskState state, PlanningDecision decision) {
        if (decision.relativePath() != null && !decision.relativePath().isBlank()) {
            state.relativePath = decision.relativePath().trim();
        }
        if (decision.content() != null && !decision.content().isBlank()) {
            state.content = decision.content();
        }
    }

    private static ToolResult createTextFile(String relativePath, String content, boolean overwrite) {
        Path targetPath;
        try {
            targetPath = resolveSafePath(relativePath);
        } catch (IllegalArgumentException ex) {
            return ToolResult.fail(ex.getMessage());
        }

        try {
            Files.createDirectories(GENERATED_FILES_DIR);
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            boolean existedBefore = Files.exists(targetPath);
            if (existedBefore && !overwrite) {
                return ToolResult.fail("文件已存在，且 overwrite=false：" + targetPath);
            }

            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            return ToolResult.success(targetPath, !existedBefore, existedBefore, content.length());
        } catch (IOException ex) {
            return ToolResult.fail("写入文件失败：" + ex.getMessage());
        }
    }

    private static Path resolveSafePath(String relativePath) {
        String cleanedPath = relativePath.trim().replace("\\", "/");
        if (cleanedPath.isBlank()) {
            throw new IllegalArgumentException("relative_path 不能为空。");
        }

        Path relative = Path.of(cleanedPath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("relative_path 不能是绝对路径。");
        }

        Path target = GENERATED_FILES_DIR.resolve(relative).toAbsolutePath().normalize();
        if (!target.startsWith(GENERATED_FILES_DIR)) {
            throw new IllegalArgumentException("不允许写入 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo4 目录之外的路径。");
        }

        return target;
    }

    private static List<ChatMessage> trimMessages(List<ChatMessage> messages, int maxTurns) {
        int maxMessageCount = maxTurns * 2;
        if (messages.size() <= maxMessageCount) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - maxMessageCount, messages.size()));
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

    private static String extractStringField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            throw new IllegalStateException("字段不是字符串：" + fieldName);
        }

        return readJsonString(json, valueStart);
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

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean startsWith(String text, int start, String expected) {
        return start >= 0
                && start + expected.length() <= text.length()
                && text.regionMatches(start, expected, 0, expected.length());
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

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

    private static void removeLastUserMessage(List<ChatMessage> messages) {
        if (!messages.isEmpty() && "user".equals(messages.get(messages.size() - 1).role())) {
            messages.remove(messages.size() - 1);
        }
    }

    public record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }

    public record PlanningDecision(
            String stepSummary,
            String action,
            String relativePath,
            String content,
            String finalResponse
    ) {}

    public static class TaskState {
        private String relativePath;
        private String content;
        private boolean fileCreated;
        private boolean overwrite = true;
        private String toolResultJson;
    }

    public record ToolResult(
            boolean ok,
            String error,
            String path,
            Boolean created,
            Boolean overwritten,
            Integer charactersWritten
    ) {
        public static ToolResult success(Path path, boolean created, boolean overwritten, int charactersWritten) {
            return new ToolResult(true, null, path.toString(), created, overwritten, charactersWritten);
        }

        public static ToolResult fail(String error) {
            return new ToolResult(false, error, null, null, null, null);
        }

        public String toJson() {
            StringBuilder builder = new StringBuilder();
            builder.append("{\"ok\":").append(ok);
            if (error != null) {
                builder.append(",\"error\":\"").append(jsonEscape(error)).append("\"");
            }
            if (path != null) {
                builder.append(",\"path\":\"").append(jsonEscape(path)).append("\"");
            }
            if (created != null) {
                builder.append(",\"created\":").append(created);
            }
            if (overwritten != null) {
                builder.append(",\"overwritten\":").append(overwritten);
            }
            if (charactersWritten != null) {
                builder.append(",\"characters_written\":").append(charactersWritten);
            }
            builder.append("}");
            return builder.toString();
        }
    }
}
