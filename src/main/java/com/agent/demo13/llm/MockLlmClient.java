package com.agent.demo13.llm;

import com.agent.demo13.context.PromptMessage;

import java.util.List;

public class MockLlmClient {

    public String complete(List<PromptMessage> messages) {
        System.out.println("\n[调试，发送给 LLM 的 JSON 请求体] ");
        System.out.println(toRequestJson(messages));

        boolean hasMemory = messages.stream().anyMatch(message -> message.role().equals("memory"));
        boolean hasToolObservation = messages.stream().anyMatch(message -> message.role().equals("tool_observation"));
        boolean hasTools = messages.stream().anyMatch(message -> message.role().equals("tools"));

        return "这是模拟 LLM 的回答：我能看到"
                + (hasMemory ? "相关长期记忆、" : "")
                + (hasToolObservation ? "必要工具结果、" : "")
                + (hasTools ? "本轮可用工具、" : "")
                + "以及当前用户问题；但看不到本地 secrets。";
    }

    /**
     * 把 List<PromptMessage> 转成符合 DeepSeek/OpenAI Chat Completions 协议的 JSON 请求体。
     * 格式示例：
     * {
     *   "model": "deepseek-v4-flash",
     *   "messages": [
     *     {"role":"system","content":"..."},
     *     {"role":"user","content":"..."}
     *   ],
     *   "stream": false,
     *   "thinking": {"type":"disabled"},
     *   "max_tokens": 200,
     *   "temperature": 0.7
     * }
     */
    public String toRequestJson(List<PromptMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"model\": \"deepseek-v4-flash\",\n");
        builder.append("  \"messages\": [\n");
        for (int i = 0; i < messages.size(); i++) {
            PromptMessage message = messages.get(i);
            builder.append("    {")
                    .append("\"role\":\"").append(escapeJson(message.role())).append("\",")
                    .append("\"content\":\"").append(escapeJson(message.content())).append("\"")
                    .append("}");
            if (i < messages.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ],\n");
        builder.append("  \"stream\": false,\n");
        builder.append("  \"thinking\": {\"type\": \"disabled\"},\n");
        builder.append("  \"max_tokens\": 200,\n");
        builder.append("  \"temperature\": 0.7\n");
        builder.append("}");
        return builder.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }
}
