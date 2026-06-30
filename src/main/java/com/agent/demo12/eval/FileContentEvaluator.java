package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileContentEvaluator implements Evaluator {

    @Override
    public String name() {
        return "file content";
    }

    /**
     * 文件内容评估，评估 agent 改过的文件里有没有期望文本
     */
    @Override
    public List<AssertionResult> evaluate(
            TestCase testCase,
            AgentRunResult result,
            Path workspaceDir,
            WorkspaceSnapshot beforeSnapshot,
            WorkspaceSnapshot afterSnapshot
    ) {
        List<AssertionResult> assertions = new ArrayList<>();
        for (TestCase.ExpectedContent expectedContent : testCase.expectedContents()) {
            Path path = workspaceDir.resolve(expectedContent.relativePath()).normalize();
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                if (content.contains(expectedContent.text())) {
                    assertions.add(AssertionResult.pass(
                            "file contains " + expectedContent.relativePath(),
                            "found expected text"
                    ));
                } else {
                    assertions.add(AssertionResult.fail(
                            "file contains " + expectedContent.relativePath(),
                            "missing text: " + expectedContent.text()
                    ));
                }
            } catch (IOException ex) {
                assertions.add(AssertionResult.fail(
                        "file contains " + expectedContent.relativePath(),
                        "read failed: " + ex.getMessage()
                ));
            }
        }
        return List.copyOf(assertions);
    }
}

