package com.agent.demo9.workflow;

import com.agent.demo9.framework.LlmClient;
import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;

public class HitlReportNode extends WorkflowNode {

    private final LlmClient llmClient;

    public HitlReportNode(String name, LlmClient llmClient) {
        super(name);
        this.llmClient = llmClient;
    }

    @Override
    public String run(WorkflowContext context) throws Exception {
        HitlWorkflowContext ctx = (HitlWorkflowContext) context;
        if (ctx.rejected()) {
            ctx.report("任务已取消：修改计划没有通过人工确认，所以没有写入任何文件。"
                    + "\n取消原因：" + (ctx.humanFeedback().isBlank() ? "未填写" : ctx.humanFeedback()));
            return "done";
        }

        String systemPrompt = "你是一个 workflow 报告生成器。请基于执行日志、审批结果、修改结果和验证结果，输出一段简洁中文总结。";
        String userContent = ""
                + "用户目标：" + ctx.goal() + "\n"
                + "意图：" + ctx.intent() + "\n"
                + "审批结果：approved=" + ctx.approved()
                + ", rejected=" + ctx.rejected()
                + ", note=" + ctx.approvalNote() + "\n"
                + "修改计划：" + ctx.patchPlan() + "\n"
                + "执行前快照：" + ctx.beforeSnapshotJson() + "\n"
                + "执行结果：" + ctx.applyResultJson() + "\n"
                + "验证结果：" + ctx.verificationResultJson() + "\n"
                + "日志：" + ctx.logs() + "\n";

        ctx.report(llmClient.askText(systemPrompt, userContent));
        return "done";
    }
}
