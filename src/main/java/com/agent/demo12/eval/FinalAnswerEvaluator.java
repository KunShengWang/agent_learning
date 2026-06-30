package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FinalAnswerEvaluator implements Evaluator {

    @Override
    public String name() {
        return "final answer";
    }

    /**
     * 最终答案评估，评估 agent 最终回答是否包含关键词
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
        for (String expectedText : testCase.expectedFinalContains()) {
            if (result.finalAnswer().contains(expectedText)) {
                assertions.add(AssertionResult.pass("final contains " + expectedText, "found"));
            } else {
                assertions.add(AssertionResult.fail(
                        "final contains " + expectedText,
                        "final answer=" + result.finalAnswer()
                ));
            }
        }
        return List.copyOf(assertions);
    }
}

