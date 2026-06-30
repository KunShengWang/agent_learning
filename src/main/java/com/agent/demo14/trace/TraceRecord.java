package com.agent.demo14.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraceRecord {

    private final String traceId;
    private final String name;
    private final String goal;
    private final Instant startedAt;
    private Instant endedAt;
    private String status = "RUNNING";
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<SpanRecord> spans = new ArrayList<>();

    public TraceRecord(String traceId, String name, String goal) {
        this.traceId = traceId;
        this.name = name;
        this.goal = goal;
        this.startedAt = Instant.now();
    }

    public void addSpan(SpanRecord span) {
        spans.add(span);
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public void end(String status) {
        this.status = status;
        this.endedAt = Instant.now();
    }

    public long durationMillis() {
        Instant end = endedAt == null ? Instant.now() : endedAt;
        return Duration.between(startedAt, end).toMillis();
    }

    public String traceId() {
        return traceId;
    }

    public String name() {
        return name;
    }

    public String goal() {
        return goal;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public String status() {
        return status;
    }

    public Map<String, String> attributes() {
        return Map.copyOf(attributes);
    }

    public List<SpanRecord> spans() {
        return List.copyOf(spans);
    }
}
