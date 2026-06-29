package com.agent.demo11.framework;

public record ToolDefinition(
        String name,
        String description,
        String parametersJson,
        ToolHandler handler
) {
}
