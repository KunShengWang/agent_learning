package com.agent.demo12.harness;

import java.nio.file.Path;

public record AgentTask(
        String caseId,
        String goal,
        Path workspaceDir
) {
}

