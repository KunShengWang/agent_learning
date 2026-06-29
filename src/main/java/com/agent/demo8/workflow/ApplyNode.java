package com.agent.demo8.workflow;

import com.agent.demo8.framework.WorkflowContext;
import com.agent.demo8.framework.WorkflowNode;
import com.agent.demo8.tools.WorkspaceTools;

import java.util.Map;

public class ApplyNode extends WorkflowNode {

    public ApplyNode(String name) {
        super(name);
    }

    @Override
    public String run(WorkflowContext context) {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;
        Map<String, String> plan = ctx.patchPlan();

        String relativePath = valueOrDefault(plan.get("relative_path"), ctx.targetFile());
        String oldText = valueOrDefault(plan.get("old_text"), "");
        String newText = valueOrDefault(plan.get("new_text"), "");
        int expectedOccurrences = parseIntOrDefault(plan.get("expected_occurrences"), 1);

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
