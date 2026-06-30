package com.agent.demo13;

import com.agent.demo13.context.ContextBuilder;
import com.agent.demo13.context.ContextBundle;
import com.agent.demo13.context.ContextDecision;
import com.agent.demo13.context.ContextPolicy;
import com.agent.demo13.context.ConversationState;
import com.agent.demo13.context.MemoryStore;
import com.agent.demo13.context.PromptMessage;
import com.agent.demo13.context.RunContext;
import com.agent.demo13.context.ToolResult;
import com.agent.demo13.llm.MockLlmClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextEngineeringDemo {

    public static void main(String[] args) throws Exception {
        // D:\JDK\IDEA\java_reinforcement_learning\agent_learning\demo13
        Path demoRoot = findDemoRoot();
        Path runRoot = demoRoot.resolve("runs").resolve("latest").normalize();
        Files.createDirectories(runRoot);

        String userGoal = "我刚学完 harness，接下来想学习 Context Engineering，请用 Java demo 解释它和 memory、RAG、tool result 的关系。";

        RunContext runContext = new RunContext(
                "user-1001",
                "agent learner",
                LocalDate.now(),
                Set.of("read_docs", "read_memory", "run_demo"),
                Map.of(
                        "DEEPSEEK_API_KEY", "sk-local-secret-never-send",
                        "DATABASE_URL", "jdbc:postgresql://internal-host/agent"
                ),
                demoRoot
        );

        // 加载长期记忆文本 long_term_memory.txt 为 MemoryItem 对象
        MemoryStore memoryStore = MemoryStore.load(demoRoot.resolve("memory").resolve("long_term_memory.txt"));
        // 构建对话状态，包含各个角色的消息和工具执行的结果信息
        ConversationState state = buildConversationState(demoRoot);

        // 构造上下文协议
        ContextPolicy policy = new ContextPolicy(
                2_800,
                4,
                3,
                420,
                Set.of("read_docs", "summarize_notes")
        );

        ContextBuilder builder = new ContextBuilder();
        ContextBundle bundle = builder.build(userGoal, runContext, state, memoryStore, policy);

        MockLlmClient llmClient = new MockLlmClient();
        String mockAnswer = llmClient.complete(bundle.messages());

        // 构建各种 Context 的报告
        String report = buildReport(runContext, bundle, mockAnswer);
        Path reportPath = runRoot.resolve("context-report.md");
        Files.writeString(reportPath, report);

        System.out.println("=== 本地 RunContext 中有，但不会全部发给 LLM ===");
        System.out.println(runContext.safeDebugView());
        System.out.println();

        System.out.println("=== 真正发送给 LLM 的 messages ===");
        for (PromptMessage message : bundle.messages()) {
            System.out.println("[" + message.role() + "]");
            System.out.println(message.content());
            System.out.println();
        }

        System.out.println("=== Context Decisions ===");
        for (ContextDecision decision : bundle.decisions()) {
            System.out.println("- " + decision.source() + " | " + decision.action() + " | " + decision.reason());
        }

        System.out.println();
        System.out.println("Mock LLM Answer: " + mockAnswer);
        System.out.println("报告文件: " + reportPath);
    }

    /**
     * 构建对话状态
     */
    private static ConversationState buildConversationState(Path demoRoot) throws Exception {
        // 对话信息包含各个角色的消息和工具执行的结果信息
        ConversationState state = new ConversationState();
        // 把各个角色的消息存入集合
        state.addUser("demo12 的 Harness 是不是主要用来评估 Agent？");
        state.addAssistant("是的，Harness 负责运行、记录、评估和生成报告。");
        state.addUser("评估结果会不会直接返回给同一次 Agent？");
        state.addAssistant("默认不会。高级系统可以把评估结果接入下一轮修复循环。");
        state.addUser("那 Context Engineering 和这些有什么关系？");

        // 读取 context-engineering-notes.md 文件内容
        String notes = Files.readString(demoRoot.resolve("docs").resolve("context-engineering-notes.md"));
        // 保存 read_docs 工具执行的结果
        state.addToolResult(new ToolResult(
                "read_docs",
                "{\"path\":\"demo13/docs/context-engineering-notes.md\"}",
                notes,
                true
        ));

        // 构建噪声输出
        String noisyOutput = "搜索结果包含很多重复内容。".repeat(60)
                + "关键结论：不要把所有历史、所有工具结果、所有本地状态都塞进 prompt。";
        state.addToolResult(new ToolResult(
                "search_docs",
                "{\"query\":\"context engineering\"}",
                noisyOutput,
                true
        ));

        return state;
    }

    private static String buildReport(RunContext runContext, ContextBundle bundle, String mockAnswer) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Demo13 Context Report\n\n");
        builder.append("## Local RunContext\n\n");
        builder.append("```text\n").append(runContext.safeDebugView()).append("\n```\n\n");
        builder.append("## Prompt Messages\n\n");
        for (PromptMessage message : bundle.messages()) {
            builder.append("### ").append(message.role()).append("\n\n");
            builder.append("```text\n").append(message.content()).append("\n```\n\n");
        }
        builder.append("## Decisions\n\n");
        builder.append("| Source | Action | Reason |\n");
        builder.append("| --- | --- | --- |\n");
        for (ContextDecision decision : bundle.decisions()) {
            builder.append("| ")
                    .append(decision.source())
                    .append(" | ")
                    .append(decision.action())
                    .append(" | ")
                    .append(decision.reason().replace("|", "\\|"))
                    .append(" |\n");
        }
        builder.append("\n## Mock LLM Answer\n\n");
        builder.append(mockAnswer).append("\n");
        return builder.toString();
    }

    private static Path findDemoRoot() {
        Path dir = Path.of("").toAbsolutePath().normalize();
        while (dir != null) {
            Path candidate = dir.resolve("demo13").normalize();
            if (Files.isDirectory(candidate.resolve("memory"))
                    && Files.isDirectory(candidate.resolve("docs"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到 demo13/memory 和 demo13/docs");
    }
}
