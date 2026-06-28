package com.agent.demo5;

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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ReActDemo {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_AGENT_LOOPS = 8;
    private static final int MAX_COMPLETION_TOKENS = 3000;
    private static final Path GENERATED_FILES_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5")
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

        System.out.println("ReAct Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我生成一份 Java 学习计划，保存成 markdown 文件，然后再读出来检查格式。");
        System.out.println("工具操作目录：" + GENERATED_FILES_DIR);
        System.out.println("当前会保留最近 " + MAX_HISTORY_TURNS + " 轮会话记忆。");

        List<ChatMessage> messages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("\n你：");
            String userGoal = reader.readLine();
            if (userGoal == null) {
                break;
            }

            // 去掉的是字符串首尾的空白字符
            userGoal = userGoal.trim();
            if (userGoal.isEmpty()) {
                System.out.println("请输入任务目标。");
                continue;
            }

            if ("exit".equalsIgnoreCase(userGoal) || "quit".equalsIgnoreCase(userGoal)) {
                System.out.println("对话结束。");
                break;
            }

            messages.add(ChatMessage.user(userGoal));
            messages = trimMessages(messages, MAX_HISTORY_TURNS);

            try {
                String finalAnswer = runReActAgent(apiKey, userGoal, messages);
                messages = trimMessages(messages, MAX_HISTORY_TURNS);
                System.out.println("\n助手：" + finalAnswer);
            } catch (IOException | InterruptedException | RuntimeException ex) {
                System.out.println("\n执行失败：" + ex.getMessage());
                removeLastUserMessage(messages);
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static String runReActAgent(String apiKey, String userGoal, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        AgentState state = new AgentState();
        state.currentGoal = userGoal;

        for (int loopIndex = 1; loopIndex <= MAX_AGENT_LOOPS; loopIndex++) {
            state.loopCount = loopIndex;

            List<ChatMessage> requestMessages = buildRuntimeMessages(messages, state);
            System.out.println("\n[调试] requestMessages = " + requestMessages);

            AssistantMessage assistantMessage = callLlm(apiKey, requestMessages);

            if (!assistantMessage.toolCalls().isEmpty()) {
                messages.add(ChatMessage.assistantToolCalls(
                        assistantMessage.content(),
                        assistantMessage.toolCalls()
                ));

                for (ToolCall toolCall : assistantMessage.toolCalls()) {
                    System.out.println("\n[循环 " + loopIndex + "] 模型选择工具：" + toolCall.name());
                    System.out.println("[工具参数] " + toolCall.argumentsJson());

                    NamedToolResult executed = executeToolCall(toolCall);
                    updateStateFromToolResult(state, executed.toolName(), executed.result());

                    String toolResultJson = executed.result().toJson();
                    System.out.println("[工具结果] " + toolResultJson);

                    messages.add(ChatMessage.tool(toolCall.id(), toolResultJson));
                }

                continue;
            }

            String finalAnswer = assistantMessage.content();
            if (finalAnswer == null || finalAnswer.isBlank()) {
                finalAnswer = "任务已处理完成。";
            }

            state.completed = true;
            messages.add(ChatMessage.assistant(finalAnswer));
            return finalAnswer;
        }

        throw new IllegalStateException("超过最大循环次数，任务仍未完成。可以把任务描述得更具体一点再重试。");
    }

    private static List<ChatMessage> buildRuntimeMessages(List<ChatMessage> userMessages, AgentState state) {
        List<ChatMessage> requestMessages = new ArrayList<>();
        requestMessages.add(createSystemMessage());
        requestMessages.add(ChatMessage.system(buildStateSummary(state)));
        requestMessages.addAll(trimMessages(userMessages, MAX_HISTORY_TURNS));
        return requestMessages;
    }

    private static ChatMessage createSystemMessage() {
        return ChatMessage.system(
                "你是一个 ReAct 风格 Agent。"
                        + "你需要结合会话记忆、当前用户目标、工具结果和运行时状态，决定下一步是否调用工具。"
                        + "如果任务还没完成，请继续调用合适工具。"
                        + "如果任务已经完成，请直接给出最终自然语言答复。"
                        + "不要假装工具已经执行成功，除非你已经看到了真实的 tool 结果。"
                        + "你拥有多个工具，需要根据任务自动选择最合适的工具。"
                        + "如果用户要求保存文件，优先使用 create_text_file。"
                        + "如果用户要求检查、核对、读取文件内容，优先使用 read_text_file。"
                        + "如果用户问当前有哪些文件，优先使用 list_files。"
                        + "所有文件都只能位于 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5 中。"
                        + "回答使用简洁清晰的中文。"
        );
    }

    private static String buildStateSummary(AgentState state) {
        return "当前任务状态摘要："
                + " current_goal=" + nullable(state.currentGoal) + ";"
                + " last_tool_name=" + nullable(state.lastToolName) + ";"
                + " last_created_path=" + nullable(state.lastCreatedPath) + ";"
                + " completed=" + state.completed + ";"
                + " loop_count=" + state.loopCount + ".";
    }

    private static AssistantMessage callLlm(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String requestBody = buildRequestBody(messages);
        System.out.println("\n[调试] requestBody = " + requestBody);

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

    private static String buildRequestBody(List<ChatMessage> messages) {
        return "{"
                + "\"model\":\"" + jsonEscape(MODEL_NAME) + "\","
                + "\"messages\":" + messagesToJson(messages) + ","
                + "\"tools\":" + buildToolsJson() + ","
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
        builder.append("\"role\":\"").append(jsonEscape(message.role())).append("\"");

        if ("tool".equals(message.role())) {
            builder.append(",\"tool_call_id\":\"")
                    .append(jsonEscape(message.toolCallId()))
                    .append("\"");
            builder.append(",\"content\":\"")
                    .append(jsonEscape(message.content()))
                    .append("\"");
        } else if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            if (message.content() == null) {
                builder.append(",\"content\":null");
            } else {
                builder.append(",\"content\":\"")
                        .append(jsonEscape(message.content()))
                        .append("\"");
            }
            builder.append(",\"tool_calls\":")
                    .append(toolCallsToJson(message.toolCalls()));
        } else {
            builder.append(",\"content\":\"")
                    .append(jsonEscape(message.content()))
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
                    .append("\"id\":\"").append(jsonEscape(toolCall.id())).append("\",")
                    .append("\"type\":\"function\",")
                    .append("\"function\":{")
                    .append("\"name\":\"").append(jsonEscape(toolCall.name())).append("\",")
                    .append("\"arguments\":\"").append(jsonEscape(toolCall.argumentsJson())).append("\"")
                    .append("}")
                    .append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private static String buildToolsJson() {
        return "["
                + buildCreateTextFileToolJson() + ","
                + buildReadTextFileToolJson() + ","
                + buildListFilesToolJson()
                + "]";
    }

    private static String buildCreateTextFileToolJson() {
        return "{"
                + "\"type\":\"function\","
                + "\"function\":{"
                + "\"name\":\"create_text_file\","
                + "\"description\":\"Create a text file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5. Use this when the user wants to save notes, outlines, plans, markdown, or code.\","
                + "\"parameters\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"relative_path\":{\"type\":\"string\",\"description\":\"Relative file path under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5.\"},"
                + "\"content\":{\"type\":\"string\",\"description\":\"Complete file content to write.\"},"
                + "\"overwrite\":{\"type\":\"boolean\",\"description\":\"Whether to overwrite the file if it already exists.\"}"
                + "},"
                + "\"required\":[\"relative_path\",\"content\",\"overwrite\"]"
                + "}"
                + "}"
                + "}";
    }

    private static String buildReadTextFileToolJson() {
        return "{"
                + "\"type\":\"function\","
                + "\"function\":{"
                + "\"name\":\"read_text_file\","
                + "\"description\":\"Read a text file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5. Use this when the user asks to inspect, verify, or review file content.\","
                + "\"parameters\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"relative_path\":{\"type\":\"string\",\"description\":\"Relative file path under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5.\"}"
                + "},"
                + "\"required\":[\"relative_path\"]"
                + "}"
                + "}"
                + "}";
    }

    private static String buildListFilesToolJson() {
        return "{"
                + "\"type\":\"function\","
                + "\"function\":{"
                + "\"name\":\"list_files\","
                + "\"description\":\"List files under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5. Use this when the user asks what files exist or wants to inspect the directory.\","
                + "\"parameters\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"relative_dir\":{\"type\":\"string\",\"description\":\"Relative directory under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5. Use '.' for the root.\"}"
                + "},"
                + "\"required\":[\"relative_dir\"]"
                + "}"
                + "}"
                + "}";
    }

    private static NamedToolResult executeToolCall(ToolCall toolCall) {
        String toolName = toolCall.name();

        try {
            if ("create_text_file".equals(toolName)) {
                String relativePath = requireStringArg(toolCall.argumentsJson(), "relative_path");
                String content = requireStringArg(toolCall.argumentsJson(), "content");
                boolean overwrite = requireBooleanArg(toolCall.argumentsJson(), "overwrite");
                return new NamedToolResult(toolName, createTextFile(relativePath, content, overwrite));
            }

            if ("read_text_file".equals(toolName)) {
                String relativePath = requireStringArg(toolCall.argumentsJson(), "relative_path");
                return new NamedToolResult(toolName, readTextFile(relativePath));
            }

            if ("list_files".equals(toolName)) {
                String relativeDir = requireStringArg(toolCall.argumentsJson(), "relative_dir");
                return new NamedToolResult(toolName, listFiles(relativeDir));
            }

            return new NamedToolResult(toolName, ToolResult.fail("未知工具：" + toolName));
        } catch (RuntimeException ex) {
            return new NamedToolResult(toolName, ToolResult.fail("工具参数解析失败：" + ex.getMessage()));
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
            return ToolResult.fileWritten(targetPath, !existedBefore, existedBefore, content.length());
        } catch (IOException ex) {
            return ToolResult.fail("写入文件失败：" + ex.getMessage());
        }
    }

    private static ToolResult readTextFile(String relativePath) {
        Path targetPath;
        try {
            targetPath = resolveSafePath(relativePath);
        } catch (IllegalArgumentException ex) {
            return ToolResult.fail(ex.getMessage());
        }

        if (!Files.exists(targetPath)) {
            return ToolResult.fail("文件不存在：" + targetPath);
        }
        if (!Files.isRegularFile(targetPath)) {
            return ToolResult.fail("目标路径不是文件：" + targetPath);
        }

        try {
            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            return ToolResult.fileRead(targetPath, content);
        } catch (IOException ex) {
            return ToolResult.fail("读取文件失败：" + ex.getMessage());
        }
    }

    private static ToolResult listFiles(String relativeDir) {
        Path targetDir;
        try {
            targetDir = resolveSafeDir(relativeDir);
        } catch (IllegalArgumentException ex) {
            return ToolResult.fail(ex.getMessage());
        }

        if (!Files.exists(targetDir)) {
            return ToolResult.fail("目录不存在：" + targetDir);
        }
        if (!Files.isDirectory(targetDir)) {
            return ToolResult.fail("目标路径不是目录：" + targetDir);
        }

        try (Stream<Path> stream = Files.list(targetDir)) {
            List<FileItem> items = stream
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .map(path -> new FileItem(
                            path.getFileName().toString(),
                            Files.isDirectory(path) ? "dir" : "file"
                    ))
                    .toList();
            return ToolResult.fileList(targetDir, items);
        } catch (IOException ex) {
            return ToolResult.fail("列出目录失败：" + ex.getMessage());
        }
    }

    private static Path resolveSafeDir(String relativeDir) {
        if (relativeDir == null || relativeDir.isBlank() || ".".equals(relativeDir.trim())) {
            return GENERATED_FILES_DIR;
        }
        return resolveSafePath(relativeDir);
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
            throw new IllegalArgumentException("不允许访问 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo5 目录之外的路径。");
        }
        return target;
    }

    private static void updateStateFromToolResult(AgentState state, String toolName, ToolResult toolResult) {
        state.lastToolName = toolName;
        state.lastToolResultJson = toolResult.toJson();

        if ("create_text_file".equals(toolName) && toolResult.ok()) {
            state.lastCreatedPath = toolResult.path();
        }

        if ("read_text_file".equals(toolName) && toolResult.ok()) {
            state.lastReadContent = toolResult.content();
        }
    }

    private static AssistantMessage parseAssistantMessage(String responseBody) {
        System.out.println("\n[调试] responseBody = " + responseBody);

        String messageObject = extractFirstMessageObject(responseBody);
        String content = extractStringField(messageObject, "content");
        String toolCallsArray = extractArrayField(messageObject, "tool_calls");

        // LLM 没返回工具调用信息
        if (toolCallsArray == null || "[]".equals(toolCallsArray.trim())) {
            return new AssistantMessage(content, List.of());
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        for (String toolCallObject : splitTopLevelObjects(toolCallsArray)) {
            String id = extractStringField(toolCallObject, "id");
            String functionObject = extractObjectField(toolCallObject, "function");
            String name = extractStringField(functionObject, "name");
            String argumentsJson = extractStringField(functionObject, "arguments");

            if (id == null || name == null || argumentsJson == null) {
                throw new IllegalStateException("tool_calls 结构不完整：" + toolCallObject);
            }
            toolCalls.add(new ToolCall(id, name, argumentsJson));
        }

        return new AssistantMessage(content, toolCalls);
    }

    private static String extractFirstMessageObject(String json) {
        int choicesIndex = json.indexOf("\"choices\"");
        if (choicesIndex < 0) {
            throw new IllegalStateException("响应中没有 choices 字段：" + json);
        }

        int choicesArrayStart = json.indexOf('[', choicesIndex);
        int firstChoiceStart = json.indexOf('{', choicesArrayStart);
        if (choicesArrayStart < 0 || firstChoiceStart < 0) {
            throw new IllegalStateException("choices 字段格式不正确：" + json);
        }

        String firstChoiceObject = readBalanced(json, firstChoiceStart, '{', '}');
        return extractObjectField(firstChoiceObject, "message");
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

    private static Boolean extractBooleanField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "true")) {
            return true;
        }
        if (startsWith(json, valueStart, "false")) {
            return false;
        }
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        throw new IllegalStateException("字段不是 boolean：" + fieldName);
    }

    private static String extractObjectField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (valueStart >= json.length() || json.charAt(valueStart) != '{') {
            throw new IllegalStateException("字段不是对象：" + fieldName);
        }

        return readBalanced(json, valueStart, '{', '}');
    }

    private static String extractArrayField(String json, String fieldName) {
        int fieldIndex = json.indexOf("\"" + fieldName + "\"");
        if (fieldIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', fieldIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (startsWith(json, valueStart, "null")) {
            return null;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '[') {
            throw new IllegalStateException("字段不是数组：" + fieldName);
        }

        return readBalanced(json, valueStart, '[', ']');
    }

    private static List<String> splitTopLevelObjects(String arrayJson) {
        List<String> objects = new ArrayList<>();
        int index = 1;
        while (index < arrayJson.length() - 1) {
            index = skipWhitespaceAndCommas(arrayJson, index);
            if (index >= arrayJson.length() - 1) {
                break;
            }
            if (arrayJson.charAt(index) != '{') {
                throw new IllegalStateException("数组元素不是对象：" + arrayJson);
            }

            int end = findMatching(arrayJson, index, '{', '}');
            objects.add(arrayJson.substring(index, end + 1));
            index = end + 1;
        }
        return objects;
    }

    private static String readBalanced(String text, int start, char open, char close) {
        int end = findMatching(text, start, open, close);
        return text.substring(start, end + 1);
    }

    private static int findMatching(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);

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

            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw new IllegalStateException("JSON 结构没有正常闭合：" + text.substring(start));
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

    private static String requireStringArg(String json, String fieldName) {
        String value = extractStringField(json, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数 " + fieldName);
        }
        return value;
    }

    private static boolean requireBooleanArg(String json, String fieldName) {
        Boolean value = extractBooleanField(json, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数 " + fieldName);
        }
        return value;
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int skipWhitespaceAndCommas(String text, int start) {
        int index = start;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (!Character.isWhitespace(current) && current != ',') {
                return index;
            }
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

    private static String nullable(String value) {
        return value == null ? "null" : "'" + value + "'";
    }

    private static List<ChatMessage> trimMessages(List<ChatMessage> messages, int maxTurns) {
        int maxMessageCount = maxTurns * 4;
        if (messages.size() <= maxMessageCount) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - maxMessageCount, messages.size()));
    }

    private static void removeLastUserMessage(List<ChatMessage> messages) {
        if (!messages.isEmpty() && "user".equals(messages.get(messages.size() - 1).role())) {
            messages.remove(messages.size() - 1);
        }
    }

    public record AssistantMessage(String content, List<ToolCall> toolCalls) {
    }

    public record ToolCall(String id, String name, String argumentsJson) {
    }

    public record NamedToolResult(String toolName, ToolResult result) {
    }

    public record FileItem(String name, String type) {
        public String toJson() {
            return "{\"name\":\"" + jsonEscape(name) + "\",\"type\":\"" + jsonEscape(type) + "\"}";
        }
    }

    public record ToolResult(
            boolean ok,
            String error,
            String path,
            Boolean created,
            Boolean overwritten,
            Integer charactersWritten,
            String content,
            Integer charactersRead,
            List<FileItem> items
    ) {
        public static ToolResult fail(String error) {
            return new ToolResult(false, error, null, null, null, null, null, null, null);
        }

        public static ToolResult fileWritten(Path path, boolean created, boolean overwritten, int charactersWritten) {
            return new ToolResult(true, null, path.toString(), created, overwritten, charactersWritten, null, null, null);
        }

        public static ToolResult fileRead(Path path, String content) {
            return new ToolResult(true, null, path.toString(), null, null, null, content, content.length(), null);
        }

        public static ToolResult fileList(Path path, List<FileItem> items) {
            return new ToolResult(true, null, path.toString(), null, null, null, null, null, List.copyOf(items));
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
            if (content != null) {
                builder.append(",\"content\":\"").append(jsonEscape(content)).append("\"");
            }
            if (charactersRead != null) {
                builder.append(",\"characters_read\":").append(charactersRead);
            }
            if (items != null) {
                builder.append(",\"items\":[");
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) {
                        builder.append(",");
                    }
                    builder.append(items.get(i).toJson());
                }
                builder.append("]");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    public record ChatMessage(
            String role,
            String content,
            String toolCallId,
            List<ToolCall> toolCalls
    ) {
        public static ChatMessage system(String content) {
            return new ChatMessage("system", content, null, List.of());
        }

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content, null, List.of());
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content, null, List.of());
        }

        public static ChatMessage assistantToolCalls(String content, List<ToolCall> toolCalls) {
            return new ChatMessage("assistant", content, null, List.copyOf(toolCalls));
        }

        public static ChatMessage tool(String toolCallId, String content) {
            return new ChatMessage("tool", content, toolCallId, List.of());
        }
    }

    public static class AgentState {
        private String currentGoal;
        private String lastToolName;
        private String lastToolResultJson;
        private String lastCreatedPath;
        private String lastReadContent;
        private boolean completed;
        private int loopCount;
    }
}
