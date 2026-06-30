package com.agent.demo12.harness;

import java.nio.file.Path;
import java.util.List;

public record CaseReport(
        String caseId,
        String goal,
        boolean agentCompleted,
        String finalAnswer,
        List<AssertionResult> assertions,
        List<TraceEvent> events,
        Path caseRunDir
) {
    public boolean passed() {
        return agentCompleted && assertions.stream().allMatch(AssertionResult::passed);
    }

    public long passedAssertionCount() {
        return assertions.stream().filter(AssertionResult::passed).count();
    }
}

