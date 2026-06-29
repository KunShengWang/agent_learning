package com.agent.demo11.mcp;

import com.agent.demo11.framework.JsonUtil;
import com.agent.demo11.framework.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

public final class McpAdapter {

    private McpAdapter() {
    }

    public static List<ToolDefinition> loadMcpTools(McpServerConfig serverConfig) {
        McpStdioClient client = new McpStdioClient(serverConfig);
        // 使用 mcp client 获取 mcp 里的所有 tools
        List<McpTool> mcpTools = client.listTools();
        System.out.println("\n[调试] mcpTools = " + mcpTools);

        List<ToolDefinition> toolDefinitions = new ArrayList<>();

        for (McpTool mcpTool : mcpTools) {
            // mcp 工具的初始名
            String originalName = mcpTool.name();
            // mcp 工具的公开名
            String exposedName = serverConfig.name() + "_" + originalName;

            toolDefinitions.add(new ToolDefinition(
                    exposedName,
                    "[MCP:" + serverConfig.name() + "] " + mcpTool.description(),
                    mcpTool.inputSchemaJson(),
                    argumentsJson -> {
                        McpCallResult result = new McpStdioClient(serverConfig)
                                .callTool(originalName, argumentsJson);
                        return "{"
                                + "\"ok\":" + result.ok() + ","
                                + "\"mcp_server\":\"" + JsonUtil.escape(serverConfig.name()) + "\","
                                + "\"mcp_tool\":\"" + JsonUtil.escape(originalName) + "\","
                                + "\"content\":\"" + JsonUtil.escape(result.content()) + "\""
                                + "}";
                    }
            ));
        }

        return toolDefinitions;
    }
}
