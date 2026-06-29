package com.agent.demo9.workflow;

import com.agent.demo9.framework.LlmClient;
import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;

import java.util.Map;

public class PlanNode extends WorkflowNode {

    private final LlmClient llmClient;

    public PlanNode(String name, LlmClient llmClient) {
        super(name);
        this.llmClient = llmClient;
    }

    @Override
    public String run(WorkflowContext context) throws Exception {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;
        String systemPrompt = """
                你是一个 workflow 规划器。
                请根据用户目标、文件快照和搜索结果，只输出 JSON 对象，不要输出 Markdown。
                字段如下：
                - relative_path: 要修改的文件相对路径
                - old_text: 文件中要被替换的精确旧文本
                - new_text: 替换后的新文本
                - expected_occurrences: 旧文本预期出现次数
                - rationale: 简短原因
                只做一个小而精确的 Java 代码改动。
                """;
        String userContent = ""
                + "用户目标：" + ctx.goal() + "\n"
                + "目标文件：" + ctx.targetFile() + "\n"
                + "文件快照：" + ctx.fileSnapshotJson() + "\n"
                + "搜索结果：" + ctx.searchResultJson() + "\n";

        Map<String, String> plan = llmClient.askJson(systemPrompt, userContent);
        ctx.patchPlan(plan);
        ctx.logs().add("plan=" + plan);
        return "approval";
    }
}
