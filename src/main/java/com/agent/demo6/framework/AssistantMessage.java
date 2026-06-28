package com.agent.demo6.framework;

import java.util.List;

public record AssistantMessage(String content, List<ToolCall> toolCalls) {
}
