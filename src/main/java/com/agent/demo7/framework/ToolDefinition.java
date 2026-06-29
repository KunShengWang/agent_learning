package com.agent.demo7.framework;

public record ToolDefinition(
        String name,
        String description,
        String parametersJson,
        ToolHandler handler
) {
}

