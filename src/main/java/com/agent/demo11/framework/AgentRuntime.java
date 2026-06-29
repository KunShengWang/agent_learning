package com.agent.demo11.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AgentRuntime {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final int maxLoops;
    private final ChatMessage systemMessage;

    public AgentRuntime(String apiKey, ToolRegistry toolRegistry, int maxLoops, ChatMessage systemMessage) {
        this.llmClient = new LlmClient(apiKey);
        this.toolRegistry = toolRegistry;
        this.maxLoops = maxLoops;
        this.systemMessage = systemMessage;
    }

    public String run(String goal, MessageStore messageStore) throws IOException, InterruptedException {
        AgentState state = new AgentState(goal);
        String toolsPayloadJson = toolRegistry.buildToolsPayloadJson();
        System.out.println("\n[调试] toolsPayloadJson = " + toolsPayloadJson);

        for (int loopIndex = 1; loopIndex <= maxLoops; loopIndex++) {
            state.loopCount(loopIndex);

            AssistantMessage assistantMessage = llmClient.call(
                    buildRuntimeMessages(messageStore, state),
                    toolsPayloadJson
            );

            if (!assistantMessage.toolCalls().isEmpty()) {
                messageStore.append(ChatMessage.assistantToolCalls(
                        assistantMessage.content(),
                        assistantMessage.toolCalls()
                ));

                for (ToolCall toolCall : assistantMessage.toolCalls()) {
                    System.out.println("\n[循环 " + loopIndex + "] 模型选择工具：" + toolCall.name());
                    System.out.println("[工具参数] " + toolCall.argumentsJson());

                    NamedToolResult result = toolRegistry.executeToolCall(toolCall);
                    state.lastToolName(result.toolName());
                    state.lastToolResultJson(result.resultJson());

                    System.out.println("[工具结果] " + result.resultJson());
                    messageStore.append(ChatMessage.tool(toolCall.id(), result.resultJson()));
                }
                continue;
            }

            String finalAnswer = assistantMessage.content();
            if (finalAnswer == null || finalAnswer.isBlank()) {
                finalAnswer = "任务已处理完成。";
            }
            messageStore.append(ChatMessage.assistant(finalAnswer));
            return finalAnswer;
        }

        throw new IllegalStateException("超过最大循环次数，任务仍未完成。");
    }

    private List<ChatMessage> buildRuntimeMessages(MessageStore messageStore, AgentState state) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(ChatMessage.user(
                "当前运行状态如下，请基于这些信息决定下一步：\n"
                        + "- goal: " + nullable(state.goal()) + "\n"
                        + "- last_tool_name: " + nullable(state.lastToolName()) + "\n"
                        + "- last_tool_result: " + (state.lastToolResultJson() == null ? "null" : state.lastToolResultJson()) + "\n"
                        + "- loop_count: " + state.loopCount() + "\n"
                        + "如果天气任务还没完成，请调用合适的 MCP 工具；如果已完成，请直接自然语言回答。"
        ));
        messages.addAll(messageStore.snapshot());
        return messages;
    }

    private static String nullable(String value) {
        return value == null ? "null" : "'" + value + "'";
    }
}
