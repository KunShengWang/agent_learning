package com.agent.demo13.context;

import java.util.List;

public record ContextBundle(
        List<PromptMessage> messages,
        List<ContextDecision> decisions,
        int estimatedChars
) {
}
