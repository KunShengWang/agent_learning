package com.agent.demo3;

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

public class ToolCallingDemo {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-v4-flash";
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final int MAX_COMPLETION_TOKENS = 4000;
    private static final Path GENERATED_FILES_DIR = Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo3")
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

        List<ChatMessage> messages = new ArrayList<>();
        // 创建系统消息
        messages.add(createSystemMessage());

        System.out.println("Tool Calling Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：创建一个Java文件，实现用for循环计算1到50的和。");
        System.out.println("工具创建的文件会保存到：" + GENERATED_FILES_DIR);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("\n你：");
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

            messages.add(ChatMessage.user(userInput));

            try {
                String answer = runAgentTurn(apiKey, messages);
                 System.out.println("\n助手：" + answer);
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

    private static ChatMessage createSystemMessage() {
        return ChatMessage.system(
                "你是一个会使用工具的 Java 和 agent 助手。"
                        + "当用户明确要求创建文件、生成文档、输出代码文件、保存内容时，"
                        + "你应该优先调用工具，而不是只在聊天中口头描述结果。"
                        + "如果需要调用工具，请基于用户需求生成合理的文件路径和完整内容。"
                        + "工具调用参数必须是完整、合法的 JSON。"
                        + "不要声称文件已经创建，除非你已经看到了真实的 tool 结果。"
                        + "所有工具创建的文件都必须位于 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo3 目录之下。"
                        + "拿到工具结果后，再用简洁清晰的中文告诉用户执行情况。"
        );
    }

    private static String runAgentTurn(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {
            // 调用 LLM 模型
            AssistantMessage assistantMessage = callLlm(apiKey, messages);
            // System.out.println("[调试] assistantMessage = " + assistantMessage);

            if (!assistantMessage.toolCalls().isEmpty()) {
                messages.add(ChatMessage.assistantToolCalls(
                        assistantMessage.content(),
                        assistantMessage.toolCalls()
                ));

                for (ToolCall toolCall : assistantMessage.toolCalls()) {
                    // System.out.println("\n[工具调用] " + toolCall.name());
                    // System.out.println("[工具参数] " + toolCall.argumentsJson());
                    // 执行 LLM 让调用的工具并返回工具执行的结果
                    ToolResult toolResult = executeToolCall(toolCall);
                    String toolResultJson = toolResult.toJson();

                    // System.out.println("[工具结果] " + toolResultJson);
                    // 把工具 id 和该工具对应的结果存入全局消息中，再后续调用 LLM 时会把该消息发送给 LLM，让 LLM 知道工具执行的结果
                    messages.add(ChatMessage.tool(
                            toolCall.id(),
                            toolResultJson
                    ));
                }

                continue;
            }

            String finalAnswer = assistantMessage.content();
            if (finalAnswer == null || finalAnswer.isBlank()) {
                finalAnswer = "我已经完成处理，但没有生成额外文本。";
            }
            messages.add(ChatMessage.assistant(finalAnswer));
            return finalAnswer;
        }

        throw new IllegalStateException("工具调用轮数过多，已停止，避免无限循环。");
    }

    /**
     * 调用 LLM 模型
     */
    private static AssistantMessage callLlm(String apiKey, List<ChatMessage> messages)
            throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // 构建请求消息
        String requestBody = buildRequestBody(messages);
        // System.out.println("\n=== requestBody ===");
        // System.out.println("requestBody : " + requestBody);

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

        // 解析助手消息并返回 AI 回答内容和调用的工具
        return parseAssistantMessage(response.body());
    }

    /**
     * 构建请求消息
     */
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
                + "{"
                + "\"type\":\"function\","
                + "\"function\":{"
                + "\"name\":\"create_text_file\","
                + "\"description\":\"Create a text-based file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo3. Use this when the user asks to create a Java file, markdown file, JSON file, config file, or any other text file.\","
                + "\"parameters\":{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "\"relative_path\":{"
                + "\"type\":\"string\","
                + "\"description\":\"Relative file path under java-demo3/generated_files, for example notes/todo.md or docs/plan.md.\""
                + "},"
                + "\"content\":{"
                + "\"type\":\"string\","
                + "\"description\":\"The complete file content to write. Keep it complete and valid, but concise unless the user asks for a long document.\""
                + "},"
                + "\"overwrite\":{"
                + "\"type\":\"boolean\","
                + "\"description\":\"Whether to overwrite the file if it already exists.\""
                + "}"
                + "},"
                + "\"required\":[\"relative_path\",\"content\",\"overwrite\"]"
                + "}"
                + "}"
                + "}"
                + "]";
    }

    private static ToolResult executeToolCall(ToolCall toolCall) {
        if (!"create_text_file".equals(toolCall.name())) {
            return ToolResult.fail("未知工具：" + toolCall.name());
        }

        String relativePath;
        String content;
        Boolean overwrite;

        try {
            // 解析 LLM 返回的 json 格式数据中的 relative_path
            relativePath = extractStringField(toolCall.argumentsJson(), "relative_path");
            // 解析 LLM 返回的 json 格式数据中的 content
            content = extractStringField(toolCall.argumentsJson(), "content");
            // 解析 LLM 返回的 json 格式数据中的 overwrite
            overwrite = extractBooleanField(toolCall.argumentsJson(), "overwrite");
        } catch (RuntimeException ex) {
            return ToolResult.fail("工具参数不是合法 JSON：" + ex.getMessage());
        }

        if (relativePath == null || relativePath.isBlank()) {
            return ToolResult.fail("缺少必要参数 relative_path。");
        }
        if (content == null) {
            return ToolResult.fail("缺少必要参数 content。");
        }
        if (overwrite == null) {
            return ToolResult.fail("缺少必要参数 overwrite。");
        }

        // 根据工具调用的内容创建文件
        return createTextFile(relativePath, content, overwrite);
    }

    /**
     * 根据工具调用的内容创建文件
     */
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
            return ToolResult.success(
                    targetPath,
                    !existedBefore,
                    existedBefore,
                    content.length()
            );
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
            throw new IllegalArgumentException("不允许写入 java-demo3/generated_files 目录之外的路径。");
        }

        return target;
    }

    /**
     * 解析助手消息并返回 AI 回答内容和调用的工具
     */
    private static AssistantMessage parseAssistantMessage(String responseBody) {
        // System.out.println("[调试] response 的 body 信息 ：" + responseBody);
        String messageObject = extractFirstMessageObject(responseBody);
        // System.out.println("[调试] messageObject = " + messageObject);
        String content = extractStringField(messageObject, "content");
        // System.out.println("[调试] content = " + content);
        String toolCallsArray = extractArrayField(messageObject, "tool_calls");
        // System.out.println("[调试] toolCallsArray = " + toolCallsArray);

        if (toolCallsArray == null || "[]".equals(toolCallsArray.trim())) {
            return new AssistantMessage(content, List.of());
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        // 把 LLM 返回的 json 中的 tool_calls 剥离出来
        List<String> toolCallList = splitTopLevelObjects(toolCallsArray);
        // System.out.println("[调试] toolCallList = " + toolCallList);
        // 收集工具调用情况，使用了哪些工具
        for (String toolCallObject : toolCallList) {
            // 要调用的工具 id
            String id = extractStringField(toolCallObject, "id");
            String functionObject = extractObjectField(toolCallObject, "function");
            // 要调用的工具 name
            String name = extractStringField(functionObject, "name");
            // LLM 返回的内容参数，包括位置、内容、覆写
            String argumentsJson = extractStringField(functionObject, "arguments");
            // 只要少一样就抛出异常，保证返回的 json 格式的完整性
            if (id == null || name == null || argumentsJson == null) {
                throw new IllegalStateException("tool_calls 结构不完整：" + toolCallObject);
            }
            toolCalls.add(new ToolCall(id, name, argumentsJson));
        }

        // 返回 AI 回答内容和调用的工具
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

    private static void removeLastUserMessage(List<ChatMessage> messages) {
        if (!messages.isEmpty() && "user".equals(messages.get(messages.size() - 1).role())) {
            messages.remove(messages.size() - 1);
        }
    }

    public record AssistantMessage(String content, List<ToolCall> toolCalls) {
    }

    public record ToolCall(String id, String name, String argumentsJson) {
    }

    public record ToolResult(
            boolean ok,
            String error,
            String path,
            Boolean created,
            Boolean overwritten,
            Integer charactersWritten
    ) {

        public static ToolResult success(
                Path path,
                boolean created,
                boolean overwritten,
                int charactersWritten
        ) {
            return new ToolResult(
                    true,
                    null,
                    path.toString(),
                    created,
                    overwritten,
                    charactersWritten
            );
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
}
