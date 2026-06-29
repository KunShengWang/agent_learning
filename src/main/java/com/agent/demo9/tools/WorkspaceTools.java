package com.agent.demo9.tools;

import com.agent.demo9.framework.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class WorkspaceTools {

    private WorkspaceTools() {
    }

    public static String listFiles(Path workspaceDir, String relativeDir) {
        Path targetDir;
        try {
            targetDir = resolveSafeDir(workspaceDir, relativeDir);
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
                        String relativePath = workspaceDir.toAbsolutePath().normalize()
                                .relativize(path.toAbsolutePath().normalize())
                                .toString()
                                .replace("\\", "/");
                        itemsJson.append("{")
                                .append("\"name\":\"").append(JsonUtil.escape(path.getFileName().toString())).append("\",")
                                .append("\"type\":\"").append(Files.isDirectory(path) ? "dir" : "file").append("\",")
                                .append("\"relative_path\":\"").append(JsonUtil.escape(relativePath)).append("\"")
                                .append("}");
                        index[0]++;
                    });
            itemsJson.append("]");
            return "{\"ok\":true,\"path\":\"" + JsonUtil.escape(targetDir.toString()) + "\",\"items\":" + itemsJson + "}";
        } catch (IOException ex) {
            return failJson("列出目录失败：" + ex.getMessage());
        }
    }

    public static String searchText(Path workspaceDir, String query, String relativeDir) {
        Path targetDir;
        try {
            targetDir = resolveSafeDir(workspaceDir, relativeDir);
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
                    String relativePath = workspaceDir.toAbsolutePath().normalize()
                            .relativize(path.toAbsolutePath().normalize())
                            .toString()
                            .replace("\\", "/");
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
                + "\"match_count\":" + matchCount
                + "}";
    }

    public static String readTextFile(Path workspaceDir, String relativePath) {
        Path targetPath;
        try {
            targetPath = resolveSafePath(workspaceDir, relativePath);
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
                    + "\"characters_read\":" + content.length()
                    + "}";
        } catch (IOException ex) {
            return failJson("读取文件失败：" + ex.getMessage());
        }
    }

    public static String replaceTextInFile(
            Path workspaceDir,
            String relativePath,
            String oldText,
            String newText,
            int expectedOccurrences
    ) {
        Path targetPath;
        try {
            targetPath = resolveSafePath(workspaceDir, relativePath);
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
                    + "\"replaced_occurrences\":" + actualOccurrences
                    + "}";
        } catch (IOException ex) {
            return failJson("替换文件内容失败：" + ex.getMessage());
        }
    }

    public static String writeTextFile(Path workspaceDir, String relativePath, String content, boolean overwrite) {
        Path targetPath;
        try {
            targetPath = resolveSafePath(workspaceDir, relativePath);
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
                    + "\"characters_written\":" + content.length()
                    + "}";
        } catch (IOException ex) {
            return failJson("写入文件失败：" + ex.getMessage());
        }
    }

    private static Path resolveSafeDir(Path workspaceDir, String relativeDir) {
        if (relativeDir == null || relativeDir.isBlank() || ".".equals(relativeDir.trim())) {
            return workspaceDir.toAbsolutePath().normalize();
        }
        return resolveSafePath(workspaceDir, relativeDir);
    }

    private static Path resolveSafePath(Path workspaceDir, String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("relative_path 不能为空。");
        }

        String cleanedPath = relativePath.trim().replace("\\", "/");
        if (cleanedPath.isBlank()) {
            throw new IllegalArgumentException("relative_path 不能为空。");
        }

        Path base = workspaceDir.toAbsolutePath().normalize();
        Path candidate = Path.of(cleanedPath);
        Path target = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : base.resolve(candidate).toAbsolutePath().normalize();

        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("不允许访问 project_workspace 目录之外的路径。");
        }
        return target;
    }

    private static int countOccurrences(String text, String query) {
        if (query == null || query.isEmpty()) {
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

    private static String failJson(String error) {
        return "{\"ok\":false,\"error\":\"" + JsonUtil.escape(error) + "\"}";
    }
}
