package com.agent.demo14.trace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TraceRecorder {

    private final Map<String, TraceRecord> traces = new LinkedHashMap<>();
    private final Map<String, SpanRecord> spans = new LinkedHashMap<>();

    public TraceRecord startTrace(String name, String goal) {
        String traceId = "trace-" + UUID.randomUUID();
        TraceRecord trace = new TraceRecord(traceId, name, goal);
        traces.put(traceId, trace);
        return trace;
    }

    public SpanHandle startSpan(
            String traceId,
            String parentSpanId,
            String name,
            String kind,
            Map<String, String> attributes
    ) {
        TraceRecord trace = trace(traceId);
        String spanId = "span-" + UUID.randomUUID();
        SpanRecord span = new SpanRecord(spanId, traceId, parentSpanId, name, kind, attributes);
        spans.put(spanId, span);
        trace.addSpan(span);
        return new SpanHandle(spanId);
    }

    public void addEvent(String spanId, String name, String detail) {
        span(spanId).addEvent(name, detail);
    }

    public void endSpan(String spanId, String status, String errorMessage) {
        span(spanId).end(status, errorMessage);
    }

    public void addTraceAttribute(String traceId, String key, String value) {
        trace(traceId).addAttribute(key, value);
    }

    public void endTrace(String traceId, String status) {
        trace(traceId).end(status);
    }

    public TraceRecord trace(String traceId) {
        TraceRecord trace = traces.get(traceId);
        if (trace == null) {
            throw new IllegalArgumentException("trace 不存在: " + traceId);
        }
        return trace;
    }

    private SpanRecord span(String spanId) {
        SpanRecord span = spans.get(spanId);
        if (span == null) {
            throw new IllegalArgumentException("span 不存在: " + spanId);
        }
        return span;
    }
}
