package com.agent.demo9.workflow;

import com.agent.demo8.framework.JsonUtil;
import com.agent.demo9.framework.WorkflowContext;
import com.agent.demo9.framework.WorkflowNode;
import com.agent.demo9.tools.WorkspaceTools;

public class InspectNode extends WorkflowNode {

    public InspectNode(String name) {
        super(name);
    }

    @Override
    public String run(WorkflowContext context) {
        CodeWorkflowContext ctx = (CodeWorkflowContext) context;

        String listing = WorkspaceTools.listFiles(ctx.workspaceDir(), ".");
        ctx.logs().add("workspace_listing=" + listing);

        if (ctx.searchQuery() != null) {
            String searchResult = WorkspaceTools.searchText(ctx.workspaceDir(), ctx.searchQuery(), ".");
            ctx.searchResultJson(searchResult);
            ctx.logs().add("search_result=" + searchResult);

            // 用搜索结果纠正 targetFile：从 matches 里取第一个 .java 文件路径
            String resolvedPath = extractFirstJavaPathFromSearch(searchResult);
            if (resolvedPath != null) {
                if (ctx.targetFile() == null || !resolvedPath.equals(ctx.targetFile())) {
                    ctx.logs().add("target_file_corrected: " + ctx.targetFile() + " -> " + resolvedPath);
                }
                ctx.targetFile(resolvedPath);
            }
        }

        if (ctx.targetFile() != null) {
            String snapshot = com.agent.demo8.tools.WorkspaceTools.readTextFile(ctx.workspaceDir(), ctx.targetFile());
            ctx.fileSnapshotJson(snapshot);
            ctx.logs().add("snapshot_path=" + com.agent.demo8.framework.JsonUtil.stringField(snapshot, "path"));

            // snapshot 读取失败时直接路由到 report，避免 PlanNode 在无文件内容下盲猜
            Boolean ok = JsonUtil.booleanField(snapshot, "ok");
            if (ok == null || !ok) {
                ctx.logs().add("snapshot_failed, routing to report");
                return "report";
            }
        } else {
            ctx.logs().add("no_target_file, routing to report");
            return "report";
        }

        if ("summary".equalsIgnoreCase(ctx.intent())) {
            return "report";
        }
        return "plan";
    }

    /**
     * 从搜索结果 JSON 的 matches 数组中提取第一个 .java 文件的 relative_path。
     */
    private static String extractFirstJavaPathFromSearch(String searchResultJson) {
        if (searchResultJson == null) {
            return null;
        }
        String key = "\"relative_path\"";
        int searchFrom = 0;
        while (true) {
            int fieldIndex = searchResultJson.indexOf(key, searchFrom);
            if (fieldIndex < 0) {
                return null;
            }
            int colonIndex = searchResultJson.indexOf(':', fieldIndex);
            if (colonIndex < 0) {
                return null;
            }
            int valueStart = colonIndex + 1;
            while (valueStart < searchResultJson.length()
                    && Character.isWhitespace(searchResultJson.charAt(valueStart))) {
                valueStart++;
            }
            if (valueStart < searchResultJson.length() && searchResultJson.charAt(valueStart) == '"') {
                String value = com.agent.demo8.framework.JsonUtil.readString(searchResultJson, valueStart);
                if (value != null && value.endsWith(".java")) {
                    return value;
                }
            }
            searchFrom = colonIndex + 1;
        }
    }
}
