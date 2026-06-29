package com.agent.demo11.framework;

import java.util.List;

public record AssistantMessage(
        String content,
        List<ToolCall> toolCalls
) {
}
