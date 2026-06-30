package com.agent.demo13.context;

public record ToolResult(
        String toolName,
        String argumentsJson,
        String content,
        boolean reFetchable// 可重新获取的
) {
}
