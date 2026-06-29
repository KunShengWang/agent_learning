package com.agent.demo7.runtime;

import com.agent.demo7.framework.*;
import com.agent.demo7.tools.CodingTools;

import java.util.Map;

public class CodingAgentRuntime extends AgentRuntime {

    public CodingAgentRuntime(String apiKey, ToolRegistry toolRegistry, int maxLoops) {
        super(apiKey, toolRegistry, maxLoops, createCodingSystemMessage());
    }

    @Override
    protected AgentState createState(String goal) {
        AgentState state = super.createState(goal);
        state.sharedContext().put("workspace_dir", CodingTools.workspaceDir().toString());
        return state;
    }

    @Override
    protected ChatMessage buildRuntimeMessage(AgentState state) {
        return ChatMessage.user(
                "你正在处理一个本地 Python 项目工作区。\n"
                        + "- goal: " + nullable(state.goal()) + "\n"
                        + "- workspace_dir: " + nullable(state.sharedContext().get("workspace_dir")) + "\n"
                        + "- shared_context: " + sharedContextJson(state.sharedContext()) + "\n"
                        + "- last_tool_name: " + nullable(state.lastToolName()) + "\n"
                        + "- last_tool_result: " + (state.lastToolResultJson() == null ? "null" : state.lastToolResultJson()) + "\n"
                        + "- completed: " + state.completed() + "\n"
                        + "- loop_count: " + state.loopCount() + "\n"
                        + "你的目标是完成一个小规模、可验证的代码任务。在修改前，先定位并读取相关代码。"
        );
    }

    private static ChatMessage createCodingSystemMessage() {
        return ChatMessage.system(
                "你是一个最小本地 coding agent。"
                        + "你的工作是先观察代码，再决定是否修改代码。"
                        + "优先使用 list_files、search_text、search_files_by_name、read_text_file 来定位相关代码。"
                        + "只有在你已经阅读并确认目标文件后，才进行修改。"
                        + "做小规模、精确的修改时优先使用 replace_text_in_file。"
                        + "如果需要创建或重写完整文件，使用 write_text_file。"
                        + "不要声称代码已修改，除非你已经看到了真实工具结果。"
                        + "当前工作区只允许在 D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo7/project_workspace 中。"
                        + "回答使用简洁清晰的中文。"
        );
    }

    private static String nullable(String value) {
        return value == null ? "null" : "'" + value + "'";
    }

    private static String sharedContextJson(Map<String, String> sharedContext) {
        StringBuilder builder = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("\"")
                    .append(JsonUtil.escape(entry.getKey()))
                    .append("\":\"")
                    .append(JsonUtil.escape(entry.getValue()))
                    .append("\"");
            index++;
        }
        builder.append("}");
        return builder.toString();
    }
}
