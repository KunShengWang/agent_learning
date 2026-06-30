package com.agent.demo12.agent;

import com.agent.demo12.harness.AgentRunResult;
import com.agent.demo12.harness.AgentTask;
import com.agent.demo12.harness.AgentUnderTest;
import com.agent.demo12.harness.TraceRecorder;
import com.agent.demo12.tools.WorkspaceTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FakeCodingAgent implements AgentUnderTest {

    @Override
    public AgentRunResult run(AgentTask task, TraceRecorder traceRecorder) throws IOException {
        // 记录 agent 开始执行
        traceRecorder.agentStarted(task.goal());
        List<AgentRunResult.ToolCallRecord> toolCalls = new ArrayList<>();

        String finalAnswer;
        if (task.goal().contains("GreetingService") || task.goal().contains("中文问候")) {
            finalAnswer = modifyGreetingService(task, traceRecorder, toolCalls);
        } else if (task.goal().contains("DiscountService") || task.goal().contains("小于 0")) {
            finalAnswer = modifyDiscountService(task, traceRecorder, toolCalls);
        } else {
            finalAnswer = "没有找到可处理的任务类型。";
        }

        // 记录 agent 完成执行
        traceRecorder.agentFinal(finalAnswer);
        return new AgentRunResult(true, finalAnswer, List.copyOf(toolCalls));
    }

    private String modifyGreetingService(
            AgentTask task,
            TraceRecorder traceRecorder,
            List<AgentRunResult.ToolCallRecord> toolCalls
    ) throws IOException {
        // 没有用到 llm 只是自己模拟的 llm 的调用过程
        String relativePath = "src/main/java/com/example/workspace/GreetingService.java";

        String content = callReadTextFile(task, traceRecorder, toolCalls, relativePath);
        System.out.println("\n[调试] content = " + content);
        System.out.println("\n[调试] traceRecorder = " + traceRecorder);

        String oldText = """
                    public String greetUser(String name) {
                        if (name == null || name.isBlank()) {
                            return "Hello, friend!";
                        }
                        return "Hello, " + name.trim() + "!";
                    }
                """;
        String newText = """
                    public String greetUser(String name) {
                        String trimmedName = name == null ? "" : name.trim();
                        if (trimmedName.isBlank()) {
                            return "你好，朋友！";
                        }
                        return "你好，" + trimmedName + "！";
                    }
                """;

        callReplaceTextInFile(task, traceRecorder, toolCalls, relativePath, oldText, newText);
        System.out.println("\n[调试] traceRecorder = " + traceRecorder);

        return "已修改 GreetingService，把英文问候改成中文问候。原文件长度: " + content.length();
    }

    private String modifyDiscountService(
            AgentTask task,
            TraceRecorder traceRecorder,
            List<AgentRunResult.ToolCallRecord> toolCalls
    ) throws IOException {
        String relativePath = "src/main/java/com/example/workspace/DiscountService.java";

        callReadTextFile(task, traceRecorder, toolCalls, relativePath);
        callReplaceTextInFile(
                task,
                traceRecorder,
                toolCalls,
                relativePath,
                "        return price * (1 - discountRate);\n",
                "        return Math.max(0.0, price * (1 - discountRate));\n"
        );
        return "已修改 DiscountService，避免折扣计算结果小于 0。";
    }

    private String callReadTextFile(
            AgentTask task,
            TraceRecorder traceRecorder,
            List<AgentRunResult.ToolCallRecord> toolCalls,
            String relativePath
    ) throws IOException {
        String toolName = "read_text_file";
        String arguments = "{\"relative_path\":\"" + relativePath + "\"}";
        // 记录 agent 工具调用情况
        traceRecorder.toolCall(toolName, arguments);
        String result = WorkspaceTools.readTextFile(task.workspaceDir(), relativePath);
        // 记录 agent 工具调用的结果情况
        traceRecorder.toolResult(toolName, "characters_read=" + result.length());
        // 调用的工具记录下来
        toolCalls.add(new AgentRunResult.ToolCallRecord(toolName, arguments, "characters_read=" + result.length() + "字符"));
        return result;
    }

    private void callReplaceTextInFile(
            AgentTask task,
            TraceRecorder traceRecorder,
            List<AgentRunResult.ToolCallRecord> toolCalls,
            String relativePath,
            String oldText,
            String newText
    ) throws IOException {
        String toolName = "replace_text_in_file";
        String arguments = "{\"relative_path\":\"" + relativePath + "\"}";
        traceRecorder.toolCall(toolName, arguments);
        String result = WorkspaceTools.replaceTextInFile(task.workspaceDir(), relativePath, oldText, newText);
        traceRecorder.toolResult(toolName, result);
        toolCalls.add(new AgentRunResult.ToolCallRecord(toolName, arguments, result));
    }
}

