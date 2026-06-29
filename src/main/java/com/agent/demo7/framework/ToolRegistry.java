package com.agent.demo7.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public void register(ToolDefinition toolDefinition) {
        tools.put(toolDefinition.name(), toolDefinition);
    }

    public void registerToolsFromClass(Class<?> toolClass) {
        for (Method method : toolClass.getDeclaredMethods()) {
            AgentTool annotation = method.getAnnotation(AgentTool.class);
            if (annotation == null) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalArgumentException("@AgentTool method must be static: " + method.getName());
            }
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                throw new IllegalArgumentException("@AgentTool method must accept one String argumentsJson: " + method.getName());
            }
            if (method.getReturnType() != String.class) {
                throw new IllegalArgumentException("@AgentTool method must return String resultJson: " + method.getName());
            }

            method.setAccessible(true);
            register(new ToolDefinition(
                    annotation.name(),
                    annotation.description(),
                    annotation.parametersJson(),
                    argumentsJson -> invokeTool(method, argumentsJson)
            ));
        }
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

    private static String invokeTool(Method method, String argumentsJson) {
        try {
            return (String) method.invoke(null, argumentsJson);
        } catch (IllegalAccessException ex) {
            return failJson("工具不可访问：" + ex.getMessage());
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            return failJson("工具抛出异常：" + target.getMessage());
        }
    }

    private static String failJson(String error) {
        return "{\"ok\":false,\"error\":\"" + JsonUtil.escape(error) + "\"}";
    }
}
