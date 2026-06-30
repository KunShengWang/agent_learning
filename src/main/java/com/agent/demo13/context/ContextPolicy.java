package com.agent.demo13.context;

import java.util.Set;

public record ContextPolicy(
        int maxPromptChars,
        int keepRecentTurns,
        int maxMemoryItems,
        int maxToolResultChars,
        Set<String> exposedTools
) {
}
