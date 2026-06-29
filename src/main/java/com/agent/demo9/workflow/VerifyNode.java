package com.agent.demo9.workflow;

import com.agent.demo9.framework.JsonUtil;
import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;
import com.agent.demo9.tools.WorkspaceTools;

public class VerifyNode extends WorkflowNode {

    public VerifyNode(String name) {
        super(name);
    }

    @Override
    public String run(WorkflowContext context) {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;

        Boolean ok = JsonUtil.booleanField(ctx.applyResultJson(), "ok");
        if (!Boolean.TRUE.equals(ok)) {
            ctx.verificationResultJson("{\"ok\":false,\"error\":\"apply failed\"}");
            return "report";
        }

        String path = JsonUtil.stringField(ctx.applyResultJson(), "path");
        if (path == null || path.isBlank()) {
            path = ctx.targetFile();
        }

        String snapshot = WorkspaceTools.readTextFile(ctx.workspaceDir(), path);
        ctx.verificationResultJson(snapshot);
        ctx.logs().add("verification=" + JsonUtil.booleanField(snapshot, "ok"));
        return "report";
    }
}
