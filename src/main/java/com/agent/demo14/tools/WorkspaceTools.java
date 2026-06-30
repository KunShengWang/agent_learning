package com.agent.demo14.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class WorkspaceTools {

    private WorkspaceTools() {
    }

    public static String readTextFile(Path workspaceDir, String relativePath) throws IOException {
        Path path = safePath(workspaceDir, relativePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static String replaceTextInFile(Path workspaceDir, String relativePath, String oldText, String newText)
            throws IOException {
        Path path = safePath(workspaceDir, relativePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (!content.contains(oldText)) {
            throw new IllegalStateException("oldText 不存在，无法精确替换: " + relativePath);
        }
        Files.writeString(path, content.replace(oldText, newText), StandardCharsets.UTF_8);
        return "replaced=true, relative_path=" + relativePath;
    }

    public static String compileWithMaven(Path workspaceDir) throws Exception {
        try {
            ProcessBuilder builder = new ProcessBuilder(command());
            builder.directory(workspaceDir.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("mvn compile timeout");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(firstLine(output));
            }
            return "compile=success";
        } finally {
            deleteGeneratedTarget(workspaceDir);
        }
    }

    private static List<String> command() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return List.of("cmd.exe", "/c", "mvn -q -DskipTests compile");
        }
        return List.of("sh", "-c", "mvn -q -DskipTests compile");
    }

    private static String firstLine(String output) {
        if (output == null || output.isBlank()) {
            return "compile failed without output";
        }
        String compact = output.replace("\r", "").trim();
        int newlineIndex = compact.indexOf('\n');
        return newlineIndex < 0 ? compact : compact.substring(0, newlineIndex);
    }

    private static Path safePath(Path workspaceDir, String relativePath) {
        Path root = workspaceDir.toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("路径越界: " + relativePath);
        }
        return target;
    }

    private static void deleteGeneratedTarget(Path workspaceDir) {
        Path targetDir = workspaceDir.resolve("target").normalize();
        if (!targetDir.startsWith(workspaceDir.toAbsolutePath().normalize())) {
            return;
        }
        if (!Files.exists(targetDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(targetDir)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Tracing demo keeps the compile result; cleanup failure should not hide it.
        }
    }
}
