package com.agent.demo12;

import com.agent.demo12.agent.FakeCodingAgent;
import com.agent.demo12.eval.AllowedChangedFilesEvaluator;
import com.agent.demo12.eval.Evaluator;
import com.agent.demo12.eval.FileContentEvaluator;
import com.agent.demo12.eval.FinalAnswerEvaluator;
import com.agent.demo12.eval.MavenCompileEvaluator;
import com.agent.demo12.eval.TrajectoryEvaluator;
import com.agent.demo12.harness.HarnessReport;
import com.agent.demo12.harness.HarnessRunner;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.TestCaseLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HarnessDemo {

    public static void main(String[] args) throws Exception {
        // 寻找 demo12 目录，demo12 目录下有 fixtures 和 test_cases 目录
        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12
        Path demoRoot = findDemoRoot();
        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12\runs\latest
        Path runRoot = demoRoot.resolve("runs").resolve("latest").normalize();

        prepareRunRoot(demoRoot, runRoot);

        // 加载所有测试用例，也就是所有 .properties 文件
        List<TestCase> testCases = new TestCaseLoader().loadAll(demoRoot.resolve("test_cases"));
        List<Evaluator> evaluators = List.of(
                new TrajectoryEvaluator(),          // 1. 轨迹评估，评估 agent 调用工具的序列对不对
                new FileContentEvaluator(),         // 2. 文件内容评估，评估 agent 改过的文件里有没有期望文本
                new AllowedChangedFilesEvaluator(), // 3. 允许改动文件评估，评估 agent 改动的文件是否在白名单内
                new MavenCompileEvaluator(),        // 4. Maven 编译评估，评估 agent 改完还能不能编译
                new FinalAnswerEvaluator()          // 5. 最终答案评估，评估 agent 最终回答是否包含关键词
        );

        HarnessRunner runner = new HarnessRunner(new FakeCodingAgent(), evaluators);
        HarnessReport report = runner.run(testCases, demoRoot, runRoot);

        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12\runs\latest\report.md
        Path reportPath = runRoot.resolve("report.md");
        Files.writeString(reportPath, report.toMarkdown());

        System.out.println(report.shortSummary());
        System.out.println("报告文件: " + reportPath);
        System.out.println("Trace 文件在: " + runRoot);
    }

    /**
     * 寻找 demo12 目录，demo12 目录下有 fixtures 和 test_cases 目录
     */
    private static Path findDemoRoot() {
        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning
        Path currentDir = Path.of("").toAbsolutePath().normalize();

        Path dir = currentDir;
        while (dir != null) {
            // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo12
            Path candidate = dir.resolve("demo12").normalize();

            if (Files.isDirectory(candidate.resolve("fixtures"))
                    && Files.isDirectory(candidate.resolve("test_cases"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到 demo12/fixtures 和 demo12/test_cases，当前工作目录: " + currentDir);
    }

    private static void prepareRunRoot(Path demoRoot, Path runRoot) throws IOException {
        Path runsDir = demoRoot.resolve("runs").normalize();
        if (!runRoot.startsWith(runsDir)) {
            throw new IllegalStateException("runRoot 不在 runs 目录内: " + runRoot);
        }
        deleteDirectory(runRoot);
        Files.createDirectories(runRoot);
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            List<Path> paths = stream
                    .sorted((left, right) -> right.compareTo(left))
                    .toList();
            for (Path path : paths) {
                Files.delete(path);
            }
        }
    }
}

