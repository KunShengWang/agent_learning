package com.agent.demo13.context;

import java.util.List;

/**
 * 上下文压缩器
 */
public class ContextCompressor {

    public String summarizeOldTurns(List<ChatTurn> oldTurns) {
        if (oldTurns.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("较早对话摘要:\n");
        for (ChatTurn turn : oldTurns) {
            builder.append("- ")
                    .append(turn.role())
                    .append(": ")
                    .append(firstSentence(turn.content()))
                    .append("\n");
        }
        return builder.toString();
    }

    private String firstSentence(String content) {
        int limit = Math.min(content.length(), 70);
        String shortText = content.substring(0, limit);
        if (content.length() > limit) {
            return shortText + "...";
        }
        return shortText;
    }
}
