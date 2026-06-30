package com.agent.demo12.harness;

import com.agent.demo12.eval.Evaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class HarnessRunner {

    private final AgentUnderTest agent;
    private final List<Evaluator> evaluators;

    public HarnessRunner(AgentUnderTest agent, List<Evaluator> evaluators) {
        this.agent = agent;
        this.evaluators = evaluators;
    }

    public HarnessReport run(List<TestCase> testCases, Path demoRoot, Path runRoot) throws IOException {
        List<CaseReport> reports = new ArrayList<>();

        for (TestCase testCase : testCases) {
            reports.add(runOne(testCase, demoRoot, runRoot));
        }

        return new HarnessReport(List.copyOf(reports), runRoot);
    }

    private CaseReport runOne(TestCase testCase, Path demoRoot, Path runRoot) throws IOException {
        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12\runs\latest\case-001-chinese-greeting
        Path caseRunDir = runRoot.resolve(testCase.id());

        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12\runs\latest\case-001-chinese-greeting\workspace
        Path workspaceDir = caseRunDir.resolve("workspace");

        Files.createDirectories(caseRunDir);
        // 把 fixtures 下的 basic-java-app 目录中的所有内容复制到 runs\latest\case-001-chinese-greeting\workspace 目录下
        copyDirectory(demoRoot.resolve("fixtures").resolve(testCase.fixtureDir()), workspaceDir);

        TraceRecorder traceRecorder = new TraceRecorder(testCase.id());
        // 记录下现在有哪些文件和各个文件的内容。测试前后各拍一张，对比哈希就能精确知道 agent 改了哪些文件，从而判断改动是否在允许范围内、是否包含期望内容。
        WorkspaceSnapshot beforeSnapshot = WorkspaceSnapshot.capture(workspaceDir);
        System.out.println("\n[调试] beforeSnapshot = " + beforeSnapshot);
        AgentRunResult agentResult;
        boolean completed;
        String finalAnswer;

        try {
            // 模拟 agent 执行
            agentResult = agent.run(new AgentTask(testCase.id(), testCase.goal(), workspaceDir), traceRecorder);
            completed = agentResult.completed();
            finalAnswer = agentResult.finalAnswer();
        } catch (Exception ex) {
            agentResult = new AgentRunResult(false, "Agent 执行异常: " + ex.getMessage(), List.of());
            completed = false;
            finalAnswer = agentResult.finalAnswer();
            traceRecorder.agentFinal(finalAnswer);
        }

        // 记录下现在有哪些文件和各个文件的内容，方便与 agent 开始执行之前的文件做对比
        WorkspaceSnapshot afterSnapshot = WorkspaceSnapshot.capture(workspaceDir);
        System.out.println("\n[调试] afterSnapshot = " + afterSnapshot);

        List<AssertionResult> assertions = new ArrayList<>();
        // 每个评估工具对 agent 的执行结果进行评估
        for (Evaluator evaluator : evaluators) {
            List<AssertionResult> results = evaluator.evaluate(
                    testCase,
                    agentResult,
                    workspaceDir,
                    beforeSnapshot,
                    afterSnapshot
            );
            assertions.addAll(results);
            for (AssertionResult result : results) {
                traceRecorder.evaluation(evaluator.name(), result.name() + ": " + (result.passed() ? "PASS" : "FAIL"));
            }
        }

        TraceExporter.writeJsonl(caseRunDir.resolve("trace.jsonl"), traceRecorder.events());
        return new CaseReport(
                testCase.id(),
                testCase.goal(),
                completed,
                finalAnswer,
                List.copyOf(assertions),
                traceRecorder.events(),
                caseRunDir
        );
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
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
}

