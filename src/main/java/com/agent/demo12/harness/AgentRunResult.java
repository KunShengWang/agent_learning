package com.agent.demo12.harness;

import java.util.List;

public record AgentRunResult(
        boolean completed,
        String finalAnswer,
        List<ToolCallRecord> toolCalls
) {
    public record ToolCallRecord(
            String name,
            String argumentsJson,
            String resultSummary
    ) {
    }
}

