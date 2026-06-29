package com.agent.demo10.rag;

public record RetrievedChunk(
        String source,
        int chunkIndex,
        String content,
        double distance
) {
}
