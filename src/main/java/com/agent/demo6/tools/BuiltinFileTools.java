package com.agent.demo6.tools;

import com.agent.demo6.framework.AgentTool;
import com.agent.demo6.framework.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 内置文件工具
 */
public final class BuiltinFileTools {

    private static final Path GENERATED_FILES_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6")
            .toAbsolutePath()
            .normalize();

    private BuiltinFileTools() {
    }

    public static Path generatedFilesDir() {
        return GENERATED_FILES_DIR;
    }

    @AgentTool(
            name = "create_text_file",
            description = "Create a text file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6. Use this when the user wants to save notes, outlines, plans, markdown, or code.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_path": {
                          "type": "string",
                          "description": "Relative file path under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6, such as notes/plan.md."
                        },
                        "content": {
                          "type": "string",
                          "description": "Complete file content to write."
                        },
                        "overwrite": {
                          "type": "boolean",
                          "description": "Whether to overwrite the file if it already exists."
                        }
                      },
                      "required": ["relative_path", "content", "overwrite"]
                    }
                    """
    )
    public static String createTextFile(String argumentsJson) {
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
            Files.createDirectories(GENERATED_FILES_DIR);
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
                    + "\"context_updates\":{"
                    + "\"last_created_relative_path\":\"" + JsonUtil.escape(relativePath) + "\""
                    + "}"
                    + "}";
        } catch (IOException ex) {
            return failJson("写入文件失败：" + ex.getMessage());
        }
    }

    @AgentTool(
            name = "read_text_file",
            description = "Read a text file under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6. Use this when the user asks to inspect, verify, or review file content.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_path": {
                          "type": "string",
                          "description": "Relative file path under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6."
                        }
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
                    + "\"context_updates\":{"
                    + "\"last_read_relative_path\":\"" + JsonUtil.escape(relativePath) + "\""
                    + "}"
                    + "}";
        } catch (IOException ex) {
            return failJson("读取文件失败：" + ex.getMessage());
        }
    }

    @AgentTool(
            name = "list_files",
            description = "List files under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6. Use this when the user asks what files exist or wants to inspect the directory.",
            parametersJson = """
                    {
                      "type": "object",
                      "properties": {
                        "relative_dir": {
                          "type": "string",
                          "description": "Relative directory under D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6. Use '.' for the root."
                        }
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
            StringBuilder itemsJson = new StringBuilder();
            itemsJson.append("[");
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

            return "{"
                    + "\"ok\":true,"
                    + "\"path\":\"" + JsonUtil.escape(targetDir.toString()) + "\","
                    + "\"items\":" + itemsJson
                    + "}";
        } catch (IOException ex) {
            return failJson("列出目录失败：" + ex.getMessage());
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
            throw new IllegalArgumentException("不允许访问 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo6 目录之外的路径。");
        }
        return target;
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

    private static String failJson(String error) {
        return "{\"ok\":false,\"error\":\"" + JsonUtil.escape(error) + "\"}";
    }
}
