package com.agent.demo11.framework;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public void register(ToolDefinition toolDefinition) {
        tools.put(toolDefinition.name(), toolDefinition);
    }

    public Set<String> toolNames() {
        return tools.keySet();
    }

    public String buildToolsPayloadJson() {
        StringBuilder builder = new StringBuilder("[");
        int index = 0;
        for (ToolDefinition tool : tools.values()) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("{")
                    .append("\"type\":\"function\",")
                    .append("\"function\":{")
                    .append("\"name\":\"").append(JsonUtil.escape(tool.name())).append("\",")
                    .append("\"description\":\"").append(JsonUtil.escape(tool.description())).append("\",")
                    .append("\"parameters\":").append(tool.parametersJson())
                    .append("}")
                    .append("}");
            index++;
        }
        builder.append("]");
        return builder.toString();
    }

    public NamedToolResult executeToolCall(ToolCall toolCall) {
        ToolDefinition tool = tools.get(toolCall.name());
        if (tool == null) {
            return new NamedToolResult(toolCall.name(), failJson("未知工具：" + toolCall.name()));
        }

        try {
            return new NamedToolResult(tool.name(), tool.handler().handle(toolCall.argumentsJson()));
        } catch (RuntimeException ex) {
            return new NamedToolResult(tool.name(), failJson("工具执行失败：" + ex.getMessage()));
        }
    }

    private static String failJson(String error) {
        return "{\"ok\":false,\"error\":\"" + JsonUtil.escape(error) + "\"}";
    }
}
