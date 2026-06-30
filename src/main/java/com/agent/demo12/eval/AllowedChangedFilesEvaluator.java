package com.agent.demo12.eval;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AssertionResult;
import com.agent.demo12.harness.TestCase;
import com.agent.demo12.harness.WorkspaceSnapshot;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;

public class AllowedChangedFilesEvaluator implements Evaluator {

    @Override
    public String name() {
        return "changed files";
    }

    /**
     * 允许改动文件评估，评估 agent 改动的文件是否在白名单内
     */
    @Override
    public List<AssertionResult> evaluate(
            TestCase testCase,
            AgentRunResult result,
            Path workspaceDir,
            WorkspaceSnapshot beforeSnapshot,
            WorkspaceSnapshot afterSnapshot
    ) {
        Set<String> changedFiles = afterSnapshot.changedFilesComparedTo(beforeSnapshot);
        Set<String> allowedFiles = new TreeSet<>(testCase.allowedChangedFiles());
        Set<String> unexpected = new TreeSet<>(changedFiles);
        unexpected.removeAll(allowedFiles);

        if (unexpected.isEmpty()) {
            return List.of(AssertionResult.pass(
                    "allowed changed files",
                    "changed=" + changedFiles
            ));
        }

        return List.of(AssertionResult.fail(
                "allowed changed files",
                "unexpected=" + unexpected + ", changed=" + changedFiles
        ));
    }
}

