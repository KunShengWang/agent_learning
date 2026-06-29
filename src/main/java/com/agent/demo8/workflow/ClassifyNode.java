package com.agent.demo8.workflow;

import com.agent.demo8.framework.LlmClient;
import com.agent.demo8.framework.WorkflowContext;
import com.agent.demo8.framework.WorkflowNode;

import java.util.Map;

public class ClassifyNode extends WorkflowNode {

    private final LlmClient llmClient;

    public ClassifyNode(String name, LlmClient llmClient) {
        super(name);
        this.llmClient = llmClient;
    }

    @Override
    public String run(WorkflowContext context) throws Exception {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;
        String systemPrompt = """
                你是一个 workflow 分类器。
                请根据用户目标只输出 JSON 对象，不要输出 Markdown。
                字段如下：
                - intent: summary 或 edit
                - target_file: 目标文件相对路径，不确定就返回空字符串
                - search_query: 搜索关键词
                - reason: 简短原因
                如果用户明显要求修改代码或文件，intent 选择 edit。
                如果用户只是想了解、总结或检查，intent 选择 summary。
                当前工作区是一个 Java Maven 项目。
                """;
        Map<String, String> data = llmClient.askJson(systemPrompt, "用户目标：" + ctx.goal());

        ctx.intent(valueOrDefault(data.get("intent"), "edit"));
        ctx.targetFile(emptyToNull(data.get("target_file")));
        ctx.searchQuery(emptyToNull(data.get("search_query")));
        ctx.logs().add("classify=" + data);
        return "inspect";
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() || "null".equals(value) ? null : value;
    }
}
