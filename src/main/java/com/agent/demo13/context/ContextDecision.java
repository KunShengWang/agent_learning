package com.agent.demo13.context;

public record ContextDecision(
        String source,
        String action,
        String reason
) {
}
