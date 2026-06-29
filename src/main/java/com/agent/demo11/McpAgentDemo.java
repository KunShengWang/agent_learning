package com.agent.demo11;

import com.agent.demo11.framework.*;
import com.agent.demo11.mcp.McpAdapter;
import com.agent.demo11.mcp.McpServerConfig;
import com.agent.demo11.mcp.WeatherMcpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class McpAgentDemo {

    private static final int MAX_AGENT_LOOPS = 5;
    private static final int MAX_HISTORY_TURNS = 6;

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        McpServerConfig weatherServer = weatherServerConfig();
        System.out.println("\n[调试] weatherServer = " + weatherServer);

        ToolRegistry registry = new ToolRegistry();
        // 加载 mcp 工具并进行工具的注册
        for (ToolDefinition tool : McpAdapter.loadMcpTools(weatherServer)) {// 加载 mcp 工具
            registry.register(tool);
        }

        AgentRuntime runtime = new AgentRuntime(
                apiKey,
                registry,
                MAX_AGENT_LOOPS,
                createMcpSystemMessage()
        );
        MessageStore messageStore = new MessageStore(MAX_HISTORY_TURNS);

        System.out.println("MCP Agent Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我查一下杭州今天的天气，并给我一个出行建议。");
        System.out.println("本节课的天气数据来自 Java WeatherMcpServer。");
        System.out.println("已注册 MCP 工具：" + registry.toolNames());

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

    /**
     * [
     *   "D:\JDK\Jdk17.0.14\bin\java",
     *   "-cp",
     *   "D:\...\agent_learning\target\classes;...",  ← 当前 classpath
     *   "com.agent.demo11.mcp.WeatherMcpServer"      ← 主类全限定名
     * ]
     */
    private static McpServerConfig weatherServerConfig() {
        // 找当前 JDK 里的 java 可执行文件绝对路径，用来当 MCP Server 的启动命令，即 D:\JDK\Jdk17.0.14\bin\java
        String javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add(WeatherMcpServer.class.getName());
        return new McpServerConfig("weather", javaCommand, args);
    }

    private static ChatMessage createMcpSystemMessage() {
        return ChatMessage.system(
                "你是一个支持 MCP 工具调用的 Agent。"
                        + "当用户询问天气、城市天气、出行建议时，优先调用 weather MCP 工具获取真实工具结果。"
                        + "如果工具返回不支持某个城市，请如实告诉用户当前支持哪些城市。"
                        + "不要伪造天气数据。回答使用简洁中文。"
        );
    }
}
