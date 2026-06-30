package com.agent.demo13.context;

import java.util.ArrayList;
import java.util.List;

public class ContextBuilder {

    private final ContextCompressor compressor = new ContextCompressor();
    private final ToolResultTrimmer trimmer = new ToolResultTrimmer();
    private final ToolCatalog toolCatalog = new ToolCatalog();

    public ContextBundle build(
            String userGoal,// 用户会话
            RunContext runContext,
            ConversationState state,// 会话历史/会话状态
            MemoryStore memoryStore,// 长期记忆文本
            ContextPolicy policy// 上下文协议
    ) {
        List<PromptMessage> messages = new ArrayList<>();
        List<ContextDecision> decisions = new ArrayList<>();

        messages.add(new PromptMessage(
                "system",
                "你是一个帮助初学者学习 Agent 的 Java 助手。回答时先讲思想，再对应 Java demo。"
        ));
        decisions.add(new ContextDecision("system instruction", "include", "最高优先级行为约束"));

        messages.add(new PromptMessage(
                "developer_context",
                "安全投影后的本地上下文:\n" + runContext.safeProjectionForPrompt()// 安全投影后的本地上下文，去除敏感信息
        ));
        decisions.add(new ContextDecision("RunContext", "project", "只把安全字段投影给 LLM，secrets 留在本地"));

        // 根据用户会话从数据库中搜寻相关的 k 个片段（相当于 rag 的功能）
        List<MemoryItem> memories = memoryStore.retrieveRelevant(userGoal, policy.maxMemoryItems());
        StringBuilder memoryText = new StringBuilder();
        for (MemoryItem memory : memories) {
            memoryText.append("- ").append(memory.content()).append("\n");
            decisions.add(new ContextDecision(memory.id(), "include", "和当前学习目标相关"));
        }
        messages.add(new PromptMessage("memory", memoryText.toString()));

        addHistory(messages, decisions, state.turns(), policy.keepRecentTurns());
        addToolResults(messages, decisions, state.toolResults(), policy.maxToolResultChars());
        addTools(messages, decisions, policy);

        messages.add(new PromptMessage("user", userGoal));
        decisions.add(new ContextDecision("current user goal", "include", "当前任务必须保留"));

        int estimatedChars = messages.stream().mapToInt(message -> message.content().length()).sum();
        if (estimatedChars > policy.maxPromptChars()) {
            decisions.add(new ContextDecision(
                    "prompt budget",
                    "warning",
                    "当前 prompt 字符数 " + estimatedChars + " 超过预算 " + policy.maxPromptChars()
            ));
        } else {
            decisions.add(new ContextDecision(
                    "prompt budget",
                    "pass",
                    "当前 prompt 字符数 " + estimatedChars + " 未超过预算 " + policy.maxPromptChars()
            ));
        }

        return new ContextBundle(List.copyOf(messages), List.copyOf(decisions), estimatedChars);
    }

    private void addHistory(
            List<PromptMessage> messages,
            List<ContextDecision> decisions,
            List<ChatTurn> turns,
            int keepRecentTurns
    ) {
        int splitIndex = Math.max(0, turns.size() - keepRecentTurns);
        List<ChatTurn> oldTurns = turns.subList(0, splitIndex);
        List<ChatTurn> recentTurns = turns.subList(splitIndex, turns.size());

        // 如果会话消息超出了最大的限制，需要进行对超出的部分进行压缩
        if (!oldTurns.isEmpty()) {
            messages.add(new PromptMessage("history_summary", compressor.summarizeOldTurns(oldTurns)));
            decisions.add(new ContextDecision("old conversation", "compress", "旧对话压缩成摘要，减少上下文占用"));
        }

        StringBuilder recent = new StringBuilder("近期对话原文:\n");
        for (ChatTurn turn : recentTurns) {
            recent.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }
        messages.add(new PromptMessage("recent_history", recent.toString()));
        decisions.add(new ContextDecision("recent conversation", "include", "最近几轮最影响当前回答"));
    }

    private void addToolResults(
            List<PromptMessage> messages,
            List<ContextDecision> decisions,
            List<ToolResult> toolResults,
            int maxToolResultChars
    ) {
        for (ToolResult toolResult : toolResults) {
            // 对工具的执行结果进行裁剪，避免工具的执行结果过长
            String trimmed = trimmer.trim(toolResult.content(), maxToolResultChars);
            messages.add(new PromptMessage(
                    "tool_observation",
                    "tool=" + toolResult.toolName() + "\nargs=" + toolResult.argumentsJson() + "\ncontent:\n" + trimmed
            ));
            decisions.add(new ContextDecision(
                    "tool result:" + toolResult.toolName(),
                    trimmed.equals(toolResult.content()) ? "include" : "trim",
                    trimmed.equals(toolResult.content())
                            ? "工具结果较短，直接保留"
                            : "工具结果过长，只保留头尾和关键片段"
            ));
        }
    }

    private void addTools(
            List<PromptMessage> messages,
            List<ContextDecision> decisions,
            ContextPolicy policy
    ) {
        // 对现有工具进行过滤，返回安全的工具
        List<ToolSpec> selectedTools = toolCatalog.selectTools(policy, decisions);
        StringBuilder text = new StringBuilder("本轮暴露给模型的工具:\n");
        for (ToolSpec tool : selectedTools) {
            text.append("- ")
                    .append(tool.name())
                    .append(": ")
                    .append(tool.description())
                    .append("，risk=")
                    .append(tool.riskLevel())
                    .append("\n");
        }
        messages.add(new PromptMessage("tools", text.toString()));
    }
}
