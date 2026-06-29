package com.agent.demo11.mcp;

public record McpTool(
        String name,// 工具名称
        String description,// 工具描述
        String inputSchemaJson// 工具参数
) {
}
