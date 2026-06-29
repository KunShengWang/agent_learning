package com.agent.demo7;

import com.agent.demo7.framework.ChatMessage;
import com.agent.demo7.framework.MessageStore;
import com.agent.demo7.framework.ToolRegistry;
import com.agent.demo7.runtime.CodingAgentRuntime;
import com.agent.demo7.tools.CodingTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CodingAgentDemo {

    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_AGENT_LOOPS = 20;

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        // 注册所以工具
        ToolRegistry registry = new ToolRegistry();
        registry.registerToolsFromClass(CodingTools.class);

        CodingAgentRuntime runtime = new CodingAgentRuntime(apiKey, registry, MAX_AGENT_LOOPS);
        MessageStore messageStore = new MessageStore(MAX_HISTORY_TURNS);

        System.out.println("Coding Agent Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我找到 greet_user 的实现，并给空名字加一个更友好的处理。");
        System.out.println("工作区目录：" + CodingTools.workspaceDir());
        System.out.println("当前会保留最近 " + MAX_HISTORY_TURNS + " 轮会话记忆。");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("\n你：");
            String userGoal = reader.readLine();
            if (userGoal == null) {
                break;
            }

            userGoal = userGoal.trim();
            if (userGoal.isEmpty()) {
                System.out.println("请输入任务目标。");
                continue;
            }

            if ("exit".equalsIgnoreCase(userGoal) || "quit".equalsIgnoreCase(userGoal)) {
                System.out.println("对话结束。");
                break;
            }

            messageStore.append(ChatMessage.user(userGoal));

            try {
                String finalAnswer = runtime.run(userGoal, messageStore);
                System.out.println("\n助手：" + finalAnswer);
            } catch (Exception ex) {
                System.out.println("\n执行失败：" + ex.getMessage());
                messageStore.removeLastIfRole("user");
            }
        }
    }
}
