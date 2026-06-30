package com.agent.demo14.trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraceAnalyzer {

    public TraceAnalysis analyze(TraceRecord trace) {
        int toolCallCount = 0;
        List<String> failedSpans = new ArrayList<>();
        Map<String, Long> durationByKind = new LinkedHashMap<>();

        for (SpanRecord span : trace.spans()) {
            if ("tool".equals(span.kind())) {
                toolCallCount++;
            }
            if (!"OK".equals(span.status())) {
                failedSpans.add(span.name() + ": " + span.errorMessage());
            }
            durationByKind.merge(span.kind(), span.durationMillis(), Long::sum);
        }

        SpanRecord slowest = trace.spans().stream()
                .max(Comparator.comparingLong(SpanRecord::durationMillis))
                .orElse(null);

        return new TraceAnalysis(
                toolCallCount,
                slowest == null ? "none" : slowest.name(),
                slowest == null ? 0 : slowest.durationMillis(),
                List.copyOf(failedSpans),
                Map.copyOf(durationByKind)
        );
    }
}
