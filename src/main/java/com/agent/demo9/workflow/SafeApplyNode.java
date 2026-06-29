package com.agent.demo9.workflow;

import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;
import com.agent.demo9.tools.WorkspaceTools;

import java.util.Map;

public class SafeApplyNode extends WorkflowNode {

    public SafeApplyNode(String name) {
        super(name);
    }

    @Override
    public String run(WorkflowContext context) {
        HitlWorkflowContext ctx = (HitlWorkflowContext) context;
        if (ctx.approvalRequired() && !ctx.approved()) {
            ctx.applyResultJson("{\"ok\":false,\"error\":\"human approval is required before applying changes\"}");
            ctx.logs().add("apply blocked: approval missing");
            return "report";
        }

        Map<String, String> plan = ctx.patchPlan();
        String relativePath = valueOrDefault(plan.get("relative_path"), ctx.targetFile());
        String oldText = valueOrDefault(plan.get("old_text"), "");
        String newText = valueOrDefault(plan.get("new_text"), "");
        int expectedOccurrences = parseIntOrDefault(plan.get("expected_occurrences"), 1);

        if (relativePath != null && !relativePath.isBlank()) {
            String beforeSnapshot = WorkspaceTools.readTextFile(ctx.workspaceDir(), relativePath);
            ctx.beforeSnapshotJson(beforeSnapshot);
            ctx.logs().add("backup_before_apply=" + beforeSnapshot);
        }

        String result;
        if (!oldText.isBlank() && !newText.isBlank()) {
            result = WorkspaceTools.replaceTextInFile(
                    ctx.workspaceDir(),
                    relativePath,
                    oldText,
                    newText,
                    expectedOccurrences
            );
        } else {
            result = WorkspaceTools.writeTextFile(
                    ctx.workspaceDir(),
                    relativePath,
                    valueOrDefault(plan.get("content"), ""),
                    true
            );
        }

        ctx.applyResultJson(result);
        ctx.logs().add("apply_result=" + result);
        return "verify";
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int parseIntOrDefault(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
