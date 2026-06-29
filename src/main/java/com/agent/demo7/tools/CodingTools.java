package com.agent.demo7.tools;

import com.agent.demo7.framework.AgentTool;
import com.agent.demo7.framework.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class CodingTools {

    // 工作区目录
    private static final Path WORKSPACE_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7/project_workspace")
            .toAbsolutePath()
            .normalize();

    private CodingTools() {
    }

    public static Path workspaceDir() {
        return WORKSPACE_DIR;
    }

    @AgentTool(
            name = "list_files",
            description = "List files under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7. Use '.' for the workspace root.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_dir": {"type": "string", "description": "Relative directory under the workspace."}
                      },
                      "required": ["relative_dir"]
                    }
                    """
    )
    public static String listFiles(String argumentsJson) {
        String relativeDir = requireString(argumentsJson, "relative_dir");
        Path targetDir;
        try {
            targetDir = resolveSafeDir(relativeDir);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        if (!Files.exists(targetDir)) {
            return failJson("目录不存在：" + targetDir);
        }
        if (!Files.isDirectory(targetDir)) {
            return failJson("目标路径不是目录：" + targetDir);
        }

        try (Stream<Path> stream = Files.list(targetDir)) {
            StringBuilder itemsJson = new StringBuilder("[");
            int[] index = {0};
            stream.sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .forEach(path -> {
                        if (index[0] > 0) {
                            itemsJson.append(",");
                        }
                        itemsJson.append("{")
                                .append("\"name\":\"").append(JsonUtil.escape(path.getFileName().toString())).append("\",")
                                .append("\"type\":\"").append(Files.isDirectory(path) ? "dir" : "file").append("\"")
                                .append("}");
                        index[0]++;
                    });
            itemsJson.append("]");
            return "{\"ok\":true,\"path\":\"" + JsonUtil.escape(targetDir.toString()) + "\",\"items\":" + itemsJson + "}";
        } catch (IOException ex) {
            return failJson("列出目录失败：" + ex.getMessage());
        }
    }

    @AgentTool(
            name = "read_text_file",
            description = "Read a text file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_path": {"type": "string", "description": "Relative file path under the workspace."}
                      },
                      "required": ["relative_path"]
                    }
                    """
    )
    public static String readTextFile(String argumentsJson) {
        String relativePath = requireString(argumentsJson, "relative_path");
        Path targetPath;
        try {
            targetPath = resolveSafePath(relativePath);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        if (!Files.exists(targetPath)) {
            return failJson("文件不存在：" + targetPath);
        }
        if (!Files.isRegularFile(targetPath)) {
            return failJson("目标路径不是文件：" + targetPath);
        }

        try {
            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            return "{"
                    + "\"ok\":true,"
                    + "\"path\":\"" + JsonUtil.escape(targetPath.toString()) + "\","
                    + "\"content\":\"" + JsonUtil.escape(content) + "\","
                    + "\"characters_read\":" + content.length() + ","
                    + "\"context_updates\":{\"last_read_relative_path\":\"" + JsonUtil.escape(relativePath) + "\"}"
                    + "}";
        } catch (IOException ex) {
            return failJson("读取文件失败：" + ex.getMessage());
        }
    }

    @AgentTool(
            name = "search_text",
            description = "Search text in files under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "query": {"type": "string", "description": "Text to search for."},
                        "relative_dir": {"type": "string", "description": "Relative directory under the workspace."}
                      },
                      "required": ["query", "relative_dir"]
                    }
                    """
    )
    public static String searchText(String argumentsJson) {
        String query = requireString(argumentsJson, "query");
        String relativeDir = requireString(argumentsJson, "relative_dir");
        Path targetDir;
        try {
            targetDir = resolveSafeDir(relativeDir);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            return failJson("搜索目录不存在或不是目录：" + targetDir);
        }

        StringBuilder matchesJson = new StringBuilder("[");
        int matchCount = 0;

        try (Stream<Path> stream = Files.walk(targetDir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.toString().toLowerCase()))
                    .toList();

            for (Path path : files) {
                String text;
                try {
                    text = Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    continue;
                }

                String[] lines = text.split("\\R", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (!lines[i].contains(query)) {
                        continue;
                    }
                    if (matchCount > 0) {
                        matchesJson.append(",");
                    }
                    String relativePath = WORKSPACE_DIR.relativize(path).toString().replace("\\", "/");
                    matchesJson.append("{")
                            .append("\"relative_path\":\"").append(JsonUtil.escape(relativePath)).append("\",")
                            .append("\"line_number\":").append(i + 1).append(",")
                            .append("\"line\":\"").append(JsonUtil.escape(lines[i])).append("\"")
                            .append("}");
                    matchCount++;
                }
            }
        } catch (IOException ex) {
            return failJson("搜索失败：" + ex.getMessage());
        }

        matchesJson.append("]");
        return "{"
                + "\"ok\":true,"
                + "\"query\":\"" + JsonUtil.escape(query) + "\","
                + "\"matches\":" + matchesJson + ","
                + "\"match_count\":" + matchCount + ","
                + "\"context_updates\":{\"last_search_query\":\"" + JsonUtil.escape(query) + "\"}"
                + "}";
    }

    @AgentTool(
            name = "search_files_by_name",
            description = "Search files by name under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "name_query": {"type": "string", "description": "Filename or partial filename to search."},
                        "relative_dir": {"type": "string", "description": "Relative directory under the workspace."}
                      },
                      "required": ["name_query", "relative_dir"]
                    }
                    """
    )
    public static String searchFilesByName(String argumentsJson) {
        String nameQuery = requireString(argumentsJson, "name_query");
        String relativeDir = requireString(argumentsJson, "relative_dir");
        Path targetDir;
        try {
            targetDir = resolveSafeDir(relativeDir);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        StringBuilder matchesJson = new StringBuilder("[");
        int matchCount = 0;

        try (Stream<Path> stream = Files.walk(targetDir)) {
            List<Path> matches = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().contains(nameQuery.toLowerCase()))
                    .sorted(Comparator.comparing(path -> path.toString().toLowerCase()))
                    .toList();

            for (Path path : matches) {
                if (matchCount > 0) {
                    matchesJson.append(",");
                }
                String relativePath = WORKSPACE_DIR.relativize(path).toString().replace("\\", "/");
                matchesJson.append("\"").append(JsonUtil.escape(relativePath)).append("\"");
                matchCount++;
            }
        } catch (IOException ex) {
            return failJson("按文件名搜索失败：" + ex.getMessage());
        }

        matchesJson.append("]");
        return "{"
                + "\"ok\":true,"
                + "\"name_query\":\"" + JsonUtil.escape(nameQuery) + "\","
                + "\"matches\":" + matchesJson + ","
                + "\"match_count\":" + matchCount + ","
                + "\"context_updates\":{\"last_file_search_query\":\"" + JsonUtil.escape(nameQuery) + "\"}"
                + "}";
    }

    @AgentTool(
            name = "replace_text_in_file",
            description = "Replace exact text inside a file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7. Use this for small, precise code edits after reading the file.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_path": {"type": "string", "description": "Relative file path under the workspace."},
                        "old_text": {"type": "string", "description": "Exact old text to replace."},
                        "new_text": {"type": "string", "description": "Exact new text to write."},
                        "expected_occurrences": {"type": "integer", "description": "Expected number of occurrences for safety."}
                      },
                      "required": ["relative_path", "old_text", "new_text", "expected_occurrences"]
                    }
                    """
    )
    public static String replaceTextInFile(String argumentsJson) {
        String relativePath = requireString(argumentsJson, "relative_path");
        String oldText = requireString(argumentsJson, "old_text");
        String newText = requireString(argumentsJson, "new_text");
        int expectedOccurrences = requireInt(argumentsJson, "expected_occurrences");

        Path targetPath;
        try {
            targetPath = resolveSafePath(relativePath);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        if (!Files.exists(targetPath)) {
            return failJson("文件不存在：" + targetPath);
        }

        try {
            String original = Files.readString(targetPath, StandardCharsets.UTF_8);
            int actualOccurrences = countOccurrences(original, oldText);
            if (actualOccurrences != expectedOccurrences) {
                return failJson("目标文本出现次数不符合预期：expected_occurrences="
                        + expectedOccurrences + ", actual_occurrences=" + actualOccurrences);
            }

            Files.writeString(targetPath, original.replace(oldText, newText), StandardCharsets.UTF_8);
            return "{"
                    + "\"ok\":true,"
                    + "\"path\":\"" + JsonUtil.escape(targetPath.toString()) + "\","
                    + "\"replaced_occurrences\":" + actualOccurrences + ","
                    + "\"context_updates\":{\"last_modified_relative_path\":\"" + JsonUtil.escape(relativePath) + "\"}"
                    + "}";
        } catch (IOException ex) {
            return failJson("替换文件内容失败：" + ex.getMessage());
        }
    }

    @AgentTool(
            name = "write_text_file",
            description = "Write a complete file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_path": {"type": "string", "description": "Relative file path under the workspace."},
                        "content": {"type": "string", "description": "Complete file content to write."},
                        "overwrite": {"type": "boolean", "description": "Whether to overwrite an existing file."}
                      },
                      "required": ["relative_path", "content", "overwrite"]
                    }
                    """
    )
    public static String writeTextFile(String argumentsJson) {
        String relativePath = requireString(argumentsJson, "relative_path");
        String content = requireString(argumentsJson, "content");
        boolean overwrite = requireBoolean(argumentsJson, "overwrite");

        Path targetPath;
        try {
            targetPath = resolveSafePath(relativePath);
        } catch (IllegalArgumentException ex) {
            return failJson(ex.getMessage());
        }

        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            boolean existedBefore = Files.exists(targetPath);
            if (existedBefore && !overwrite) {
                return failJson("文件已存在，且 overwrite=false：" + targetPath);
            }

            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            return "{"
                    + "\"ok\":true,"
                    + "\"path\":\"" + JsonUtil.escape(targetPath.toString()) + "\","
                    + "\"created\":" + !existedBefore + ","
                    + "\"overwritten\":" + existedBefore + ","
                    + "\"characters_written\":" + content.length() + ","
                    + "\"context_updates\":{\"last_modified_relative_path\":\"" + JsonUtil.escape(relativePath) + "\"}"
                    + "}";
        } catch (IOException ex) {
            return failJson("写入文件失败：" + ex.getMessage());
        }
    }

    private static Path resolveSafeDir(String relativeDir) {
        if (relativeDir == null || relativeDir.isBlank() || ".".equals(relativeDir.trim())) {
            return WORKSPACE_DIR;
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

        Path target = WORKSPACE_DIR.resolve(relative).toAbsolutePath().normalize();
        if (!target.startsWith(WORKSPACE_DIR)) {
            throw new IllegalArgumentException("不允许访问 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7 目录之外的路径。");
        }
        return target;
    }

    private static int countOccurrences(String text, String query) {
        if (query.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(query, index)) >= 0) {
            count++;
            index += query.length();
        }
        return count;
    }

    private static String requireString(String argumentsJson, String fieldName) {
        String value = JsonUtil.stringField(argumentsJson, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数 " + fieldName);
        }
        return value;
    }

    private static boolean requireBoolean(String argumentsJson, String fieldName) {
        Boolean value = JsonUtil.booleanField(argumentsJson, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数 " + fieldName);
        }
        return value;
    }

    private static int requireInt(String argumentsJson, String fieldName) {
        Integer value = JsonUtil.intField(argumentsJson, fieldName);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数 " + fieldName);
        }
        return value;
    }

    private static String failJson(String error) {
        return "{\"ok\":false,\"error\":\"" + JsonUtil.escape(error) + "\"}";
    }
}
