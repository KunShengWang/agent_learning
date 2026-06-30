package com.agent.demo14;

import com.agent.demo14.agent.TracedCodingAgent;
import com.agent.demo14.trace.TraceAnalysis;
import com.agent.demo14.trace.TraceAnalyzer;
import com.agent.demo14.trace.TraceExporter;
import com.agent.demo14.trace.TraceRecorder;
import com.agent.demo14.trace.TraceRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class TracingDemo {

    public static void main(String[] args) throws Exception {
        Path demoRoot = findDemoRoot();
        Path runRoot = demoRoot.resolve("runs").resolve("latest").normalize();
        prepareRunRoot(demoRoot, runRoot);
        Path workspaceDir = runRoot.resolve("workspace");
        copyDirectory(demoRoot.resolve("workspace"), workspaceDir);

        String goal = Files.readString(demoRoot.resolve("tasks").resolve("coding-task.txt"));
        TraceRecorder recorder = new TraceRecorder();
        TraceRecord trace = recorder.startTrace("demo14-tracing-run", goal);

        TracedCodingAgent agent = new TracedCodingAgent(workspaceDir);
        String finalAnswer = agent.run(goal, recorder, trace.traceId());
        recorder.addTraceAttribute(trace.traceId(), "final_answer", finalAnswer);
        recorder.endTrace(trace.traceId(), "OK");

        TraceRecord completedTrace = recorder.trace(trace.traceId());
        TraceAnalysis analysis = new TraceAnalyzer().analyze(completedTrace);

        Path jsonlPath = runRoot.resolve("trace.jsonl");
        Path reportPath = runRoot.resolve("trace-report.md");
        TraceExporter exporter = new TraceExporter();
        exporter.writeJsonl(completedTrace, jsonlPath);
        exporter.writeMarkdown(completedTrace, analysis, reportPath);

        System.out.println("=== Trace Summary ===");
        System.out.println("traceId=" + completedTrace.traceId());
        System.out.println("status=" + completedTrace.status());
        System.out.println("durationMs=" + completedTrace.durationMillis());
        System.out.println("spanCount=" + completedTrace.spans().size());
        System.out.println("toolCallCount=" + analysis.toolCallCount());
        System.out.println("slowestSpan=" + analysis.slowestSpanName() + " (" + analysis.slowestSpanMillis() + " ms)");
        System.out.println("report=" + reportPath);
        System.out.println("jsonl=" + jsonlPath);
    }

    private static Path findDemoRoot() {
        Path dir = Path.of("").toAbsolutePath().normalize();
        while (dir != null) {
            Path candidate = dir.resolve("demo14").normalize();
            if (Files.isDirectory(candidate.resolve("tasks"))
                    && Files.isDirectory(candidate.resolve("workspace"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到 demo14/tasks 和 demo14/workspace");
    }

    private static void prepareRunRoot(Path demoRoot, Path runRoot) throws Exception {
        Path runsDir = demoRoot.resolve("runs").normalize();
        if (!runRoot.startsWith(runsDir)) {
            throw new IllegalStateException("runRoot 不在 runs 目录内: " + runRoot);
        }
        deleteDirectory(runRoot);
        Files.createDirectories(runRoot);
    }

    private static void copyDirectory(Path sourceDir, Path targetDir) throws Exception {
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            List<Path> paths = stream.sorted(Comparator.naturalOrder()).toList();
            for (Path source : paths) {
                Path target = targetDir.resolve(sourceDir.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(source, target);
                }
            }
        }
    }

    private static void deleteDirectory(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
