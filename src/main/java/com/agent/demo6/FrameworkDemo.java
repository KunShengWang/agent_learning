package com.agent.demo6;

import com.agent.demo6.framework.AgentRuntime;
import com.agent.demo6.framework.ChatMessage;
import com.agent.demo6.framework.MessageStore;
import com.agent.demo6.framework.RuntimeFactory;
import com.agent.demo6.tools.BuiltinFileTools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FrameworkDemo {

    private static final int MAX_HISTORY_TURNS = 6;

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        RuntimeFactory.RuntimeBundle bundle = RuntimeFactory.createRuntime(
                apiKey,
                MAX_HISTORY_TURNS,
                BuiltinFileTools.class
        );
        AgentRuntime runtime = bundle.runtime();
        MessageStore messageStore = bundle.messageStore();

        System.out.println("Framework Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我生成一份 Java Agent 学习计划，保存成 markdown 文件，然后再读出来检查格式。");
        System.out.println("工具操作目录：" + BuiltinFileTools.generatedFilesDir());
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
