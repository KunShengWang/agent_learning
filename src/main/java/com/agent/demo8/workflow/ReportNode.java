package com.agent.demo8.workflow;

import com.agent.demo8.framework.LlmClient;
import com.agent.demo8.framework.WorkflowContext;
import com.agent.demo8.framework.WorkflowNode;

public class ReportNode extends WorkflowNode {

    private final LlmClient llmClient;

    public ReportNode(String name, LlmClient llmClient) {
        super(name);
        this.llmClient = llmClient;
    }

    @Override
    public String run(WorkflowContext context) throws Exception {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;
        String systemPrompt = "你是一个 workflow 报告生成器。基于执行日志、修改结果和验证结果，输出一段简洁中文总结。";
        String userContent = ""
                + "用户目标：" + ctx.goal() + "\n"
                + "意图：" + ctx.intent() + "\n"
                + "修改计划：" + ctx.patchPlan() + "\n"
                + "执行结果：" + ctx.applyResultJson() + "\n"
                + "验证结果：" + ctx.verificationResultJson() + "\n"
                + "日志：" + ctx.logs() + "\n";

        ctx.report(llmClient.askText(systemPrompt, userContent));
        return "done";
    }
}
