package com.agent.demo11.framework;

import java.util.ArrayList;
import java.util.List;

public class MessageStore {

    private final int maxTurns;
    private final List<ChatMessage> messages = new ArrayList<>();

    public MessageStore(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public void append(ChatMessage message) {
        messages.add(message);
        trim();
    }

    public List<ChatMessage> snapshot() {
        return new ArrayList<>(messages);
    }

    public void removeLastIfRole(String role) {
        if (!messages.isEmpty() && role.equals(messages.get(messages.size() - 1).role())) {
            messages.remove(messages.size() - 1);
        }
    }

    private void trim() {
        int maxMessages = Math.max(1, maxTurns * 3);
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }
}
