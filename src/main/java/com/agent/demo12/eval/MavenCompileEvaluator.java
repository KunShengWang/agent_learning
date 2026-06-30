package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MavenCompileEvaluator implements Evaluator {

    @Override
    public String name() {
        return "maven compile";
    }

    /**
     * Maven 编译评估，评估 agent 改完还能不能编译
     */
    @Override
    public List<AssertionResult> evaluate(
            TestCase testCase,
            AgentRunResult result,
            Path workspaceDir,
            WorkspaceSnapshot beforeSnapshot,
            WorkspaceSnapshot afterSnapshot
    ) {
        if (!testCase.compileRequired()) {
            return List.of();
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(command());
            builder.directory(workspaceDir.toFile());
            builder.redirectErrorStream(true);

            Process process = builder.start();
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!finished) {
                process.destroyForcibly();
                return List.of(AssertionResult.fail("mvn compile", "timeout after 45 seconds"));
            }

            if (process.exitValue() == 0) {
                return List.of(AssertionResult.pass("mvn compile", "compile success"));
            }

            return List.of(AssertionResult.fail("mvn compile", firstLine(output)));
        } catch (IOException ex) {
            return List.of(AssertionResult.fail("mvn compile", "start failed: " + ex.getMessage()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of(AssertionResult.fail("mvn compile", "interrupted"));
        } finally {
            deleteGeneratedTarget(workspaceDir);
        }
    }

    private List<String> command() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return List.of("cmd.exe", "/c", "mvn -q -DskipTests compile");
        }
        return List.of("sh", "-c", "mvn -q -DskipTests compile");
    }

    private String firstLine(String output) {
        if (output == null || output.isBlank()) {
            return "compile failed without output";
        }
        String compact = output.replace("\r", "").trim();
        int newlineIndex = compact.indexOf('\n');
        if (newlineIndex < 0) {
            return compact;
        }
        return compact.substring(0, newlineIndex);
    }

    private void deleteGeneratedTarget(Path workspaceDir) {
        Path targetDir = workspaceDir.resolve("target").normalize();
        if (!targetDir.startsWith(workspaceDir.normalize())) {
            return;
        }
        if (!Files.exists(targetDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(targetDir)) {
            List<Path> paths = stream
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Harness 评估报告不应该因为清理编译产物失败而覆盖真正的编译结果。
        }
    }
}

