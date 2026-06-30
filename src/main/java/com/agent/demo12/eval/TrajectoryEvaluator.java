package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.List;

public class TrajectoryEvaluator implements Evaluator {

    @Override
    public String name() {
        return "trajectory";
    }

    /**
     * 轨迹评估，评估 agent 调用工具的序列对不对
     */
    @Override
    public List<AssertionResult> evaluate(
            TestCase testCase,
            AgentRunResult result,
            Path workspaceDir,
            WorkspaceSnapshot beforeSnapshot,
            WorkspaceSnapshot afterSnapshot
    ) {
        // 收集工具名称
        List<String> actual = result.toolCalls().stream()
                .map(AgentRunResult.ToolCallRecord::name)
                .toList();

        // 判断下 agent 调用的工具是否是期望 agent 调用的工具
        if (actual.equals(testCase.expectedToolSequence())) {
            return List.of(AssertionResult.pass(
                    "tool trajectory",
                    "actual=" + actual
            ));
        }

        return List.of(AssertionResult.fail(
                "tool trajectory",
                "expected=" + testCase.expectedToolSequence() + ", actual=" + actual
        ));
    }
}

