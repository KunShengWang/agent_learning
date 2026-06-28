package com.agent.demo6.framework;

public record ToolDefinition(
        String name,// 工具名称
        String description,// 工具描述
        String parametersJson,// LLM 工具调用时需要 LLM 返回的参数格式
        ToolHandler handler
) {}
