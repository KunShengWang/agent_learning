package com.agent.demo14.trace;

import java.util.List;
import java.util.Map;

public record TraceAnalysis(
        int toolCallCount,
        String slowestSpanName,
        long slowestSpanMillis,
        List<String> failedSpans,
        Map<String, Long> durationByKind
) {
}
