package com.agent.demo7.framework;

import java.util.List;

public record AssistantMessage(String content, List<ToolCall> toolCalls) {
}

