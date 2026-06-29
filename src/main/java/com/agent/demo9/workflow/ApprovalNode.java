package com.agent.demo9.workflow;

import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

public class ApprovalNode extends WorkflowNode {

    private final BufferedReader reader;

    public ApprovalNode(String name, BufferedReader reader) {
        super(name);
        this.reader = reader;
    }

    @Override
    public String run(WorkflowContext context) throws IOException {
        HitlWorkflowContext ctx = (HitlWorkflowContext) context;
        if (!ctx.approvalRequired()) {
            ctx.approved(true);
            ctx.logs().add("approval skipped");
            return "approved";
        }

        if ("summary".equalsIgnoreCase(ctx.intent())) {
            ctx.logs().add("approval skipped for summary task");
            return "approved";
        }

        Map<String, String> plan = ctx.patchPlan();
        String relativePath = valueOrDefault(plan.get("relative_path"), ctx.targetFile());
        String oldText = valueOrDefault(plan.get("old_text"), "");
        String newText = valueOrDefault(plan.get("new_text"), "");
        String rationale = valueOrDefault(plan.get("rationale"), "模型没有提供修改理由。");

        System.out.println("\n--- 待确认的修改计划 ---");
        System.out.println("目标文件：" + relativePath);
        System.out.println("修改理由：" + rationale);

        if (!oldText.isBlank()) {
            System.out.println("\n将被替换的旧内容：");
            System.out.println(previewText(oldText));
        }

        if (!newText.isBlank()) {
            System.out.println("\n准备写入的新内容：");
            System.out.println(previewText(newText));
        }

        System.out.print("\n是否允许执行这个修改？输入 yes 执行，其他内容取消：");
        String answer = reader.readLine();
        if (answer != null && ("yes".equalsIgnoreCase(answer.trim()) || "y".equalsIgnoreCase(answer.trim()))) {
            ctx.approved(true);
            ctx.approvalNote("human approved");
            ctx.logs().add("approval=approved");
            return "approved";
        }

        ctx.rejected(true);
        ctx.approvalNote("human rejected");
        System.out.print("可以输入取消原因，直接回车跳过：");
        String feedback = reader.readLine();
        ctx.humanFeedback(feedback == null ? "" : feedback.trim());
        ctx.logs().add("approval=rejected, feedback=" + ctx.humanFeedback());
        return "rejected";
    }

    private static String previewText(String value) {
        int maxLength = 500;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n...省略...";
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
