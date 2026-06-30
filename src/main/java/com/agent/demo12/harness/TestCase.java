package com.agent.demo12.harness;

import java.util.List;

public record TestCase(
        String id,
        String fixtureDir,
        String goal,
        List<String> expectedToolSequence,
        List<String> allowedChangedFiles,
        List<String> expectedFinalContains,
        boolean compileRequired,
        List<ExpectedContent> expectedContents
) {
    public record ExpectedContent(
            String relativePath,
            String text
    ) {
    }
}

