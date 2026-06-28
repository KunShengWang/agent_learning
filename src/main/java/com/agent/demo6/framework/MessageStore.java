package com.agent.demo6.framework;

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

    public void extend(List<ChatMessage> newMessages) {
        messages.addAll(newMessages);
        trim();
    }

    public List<ChatMessage> snapshot() {
        return List.copyOf(messages);
    }

    public void removeLastIfRole(String role) {
        if (!messages.isEmpty() && role.equals(messages.get(messages.size() - 1).role())) {
            messages.remove(messages.size() - 1);
        }
    }

    private void trim() {
        int maxMessageCount = maxTurns * 4;
        if (messages.size() <= maxMessageCount) {
            return;
        }

        List<ChatMessage> recent = new ArrayList<>(
                messages.subList(messages.size() - maxMessageCount, messages.size())
        );
        messages.clear();
        messages.addAll(recent);
    }
}
