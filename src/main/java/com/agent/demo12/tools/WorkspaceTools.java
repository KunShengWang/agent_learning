package com.agent.demo12.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkspaceTools {

    private WorkspaceTools() {
    }

    public static String readTextFile(Path workspaceDir, String relativePath) throws IOException {
        Path path = resolveSafePath(workspaceDir, relativePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static String replaceTextInFile(Path workspaceDir, String relativePath, String oldText, String newText)
            throws IOException {
        Path path = resolveSafePath(workspaceDir, relativePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (!content.contains(oldText)) {
            throw new IllegalStateException("oldText 不存在，无法执行精确替换: " + relativePath);
        }
        String updated = content.replace(oldText, newText);
        Files.writeString(path, updated, StandardCharsets.UTF_8);
        return "replaced=true, relative_path=" + relativePath;
    }

    private static Path resolveSafePath(Path workspaceDir, String relativePath) {
        Path normalizedWorkspace = workspaceDir.toAbsolutePath().normalize();
        Path target = normalizedWorkspace.resolve(relativePath).normalize();
        if (!target.startsWith(normalizedWorkspace)) {
            throw new IllegalArgumentException("路径越界: " + relativePath);
        }
        return target;
    }
}

