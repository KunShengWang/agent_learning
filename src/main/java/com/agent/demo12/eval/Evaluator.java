package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.List;

public interface Evaluator {

    String name();

    List<AssertionResult> evaluate(
            TestCase testCase,
            AgentRunResult result,
            Path workspaceDir,
            WorkspaceSnapshot beforeSnapshot,
            WorkspaceSnapshot afterSnapshot
    );
}

