package com.agent.demo11.mcp;

import java.util.List;

public record McpServerConfig(
        String name,
        String command,
        List<String> args
) {
}
