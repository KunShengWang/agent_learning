package com.agent.demo11.framework;

public record ToolCall(
        String id,
        String name,
        String argumentsJson
) {
}
