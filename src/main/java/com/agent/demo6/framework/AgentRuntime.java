package com.agent.demo6.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentRuntime {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final int maxLoops;
    private final ChatMessage systemMessage;

    public AgentRuntime(
            String apiKey,
            ToolRegistry toolRegistry,
            int maxLoops,
            ChatMessage systemMessage
    ) {
        this.llmClient = new LlmClient(apiKey);
        this.toolRegistry = toolRegistry;
        this.maxLoops = maxLoops;
        this.systemMessage = systemMessage == null ? defaultSystemMessage() : systemMessage;
    }

    public String run(String goal, MessageStore messageStore) throws IOException, InterruptedException {
        AgentState state = createState(goal);
        // 把工具信息拼接成 json 形式
        String toolsPayloadJson = toolRegistry.buildToolsPayloadJson();

        for (int loopIndex = 1; loopIndex <= maxLoops; loopIndex++) {
            state.loopCount(loopIndex);

            List<ChatMessage> requestMessages = buildRuntimeMessages(messageStore, state);
            AssistantMessage assistantMessage = llmClient.call(requestMessages, toolsPayloadJson);

            if (!assistantMessage.toolCalls().isEmpty()) {
                messageStore.append(ChatMessage.assistantToolCalls(
                        assistantMessage.content(),
                        assistantMessage.toolCalls()
                ));

                for (ToolCall toolCall : assistantMessage.toolCalls()) {
                    System.out.println("\n[循环 " + loopIndex + "] 模型选择工具：" + toolCall.name());
                    System.out.println("[工具参数] " + toolCall.argumentsJson());

                    NamedToolResult result = toolRegistry.executeToolCall(toolCall);
                    updateStateFromToolResult(state, result);
                    onToolResult(state, result, messageStore);

                    System.out.println("[工具结果] " + result.resultJson());

                    messageStore.append(ChatMessage.tool(
                            toolCall.id(),
                            result.resultJson()
                    ));
                }

                continue;
            }

            String finalAnswer = assistantMessage.content();
            if (finalAnswer == null || finalAnswer.isBlank()) {
                finalAnswer = "任务已处理完成。";
            }

            state.completed(true);
            messageStore.append(ChatMessage.assistant(finalAnswer));
            return finalAnswer;
        }

        throw new IllegalStateException("超过最大循环次数，任务仍未完成。可以把任务描述得更具体一点再重试。");
    }

    protected AgentState createState(String goal) {
        return new AgentState(goal);
    }

    protected List<ChatMessage> buildRuntimeMessages(MessageStore messageStore, AgentState state) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(buildRuntimeMessage(state));
        messages.addAll(messageStore.snapshot());
        return messages;
    }

    protected ChatMessage buildRuntimeMessage(AgentState state) {
        return ChatMessage.user(
                "当前运行时状态如下，请基于这些信息决定下一步：\n"
                        + "- goal: " + nullable(state.goal()) + "\n"
                        + "- shared_context: " + sharedContextJson(state.sharedContext()) + "\n"
                        + "- last_tool_name: " + nullable(state.lastToolName()) + "\n"
                        + "- last_tool_result: " + (state.lastToolResultJson() == null ? "null" : state.lastToolResultJson()) + "\n"
                        + "- completed: " + state.completed() + "\n"
                        + "- loop_count: " + state.loopCount() + "\n"
                        + "如果任务还没完成，就继续调用合适工具；如果任务已完成，就直接自然语言回答。"
        );
    }

    protected void updateStateFromToolResult(AgentState state, NamedToolResult result) {
        state.lastToolName(result.toolName());
        state.lastToolResultJson(result.resultJson());

        String contextUpdates = JsonUtil.objectField(result.resultJson(), "context_updates");
        if (contextUpdates != null) {
            state.sharedContext().putAll(JsonUtil.flatStringMap(contextUpdates));
        }
    }

    protected void onToolResult(AgentState state, NamedToolResult result, MessageStore messageStore) {
        // Hook for later demos.
    }

    private static ChatMessage defaultSystemMessage() {
        return ChatMessage.system(
                "你是一个基于小型 Agent Runtime 运行的 ReAct 风格 Agent。"
                        + "你需要结合用户目标、会话消息、工具结果和运行时状态，自主决定下一步。"
                        + "如果任务需要外部动作，请调用合适工具。"
                        + "如果任务已经完成，请直接输出最终自然语言答复。"
                        + "不要声称工具已执行成功，除非你已经看到了真实的 tool 结果。"
                        + "回答使用简洁清晰的中文。"
        );
    }

    private static String sharedContextJson(Map<String, String> sharedContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        int index = 0;
        for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("\"")
                    .append(JsonUtil.escape(entry.getKey()))
                    .append("\":\"")
                    .append(JsonUtil.escape(entry.getValue()))
                    .append("\"");
            index++;
        }
        builder.append("}");
        return builder.toString();
    }

    private static String nullable(String value) {
        return value == null ? "null" : "'" + value + "'";
    }
}
