package com.agent.demo13.context;

import java.util.ArrayList;
import java.util.List;

public class ConversationState {

    private final List<ChatTurn> turns = new ArrayList<>();
    private final List<ToolResult> toolResults = new ArrayList<>();

    public void addUser(String content) {
        turns.add(new ChatTurn("user", content));
    }

    public void addAssistant(String content) {
        turns.add(new ChatTurn("assistant", content));
    }

    public void addToolResult(ToolResult toolResult) {
        toolResults.add(toolResult);
    }

    public List<ChatTurn> turns() {
        return List.copyOf(turns);
    }

    public List<ToolResult> toolResults() {
        return List.copyOf(toolResults);
    }
}
