package com.agent.demo14.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpanRecord {

    private final String spanId;
    private final String traceId;
    private final String parentSpanId;
    private final String name;
    private final String kind;
    private final Instant startedAt;
    private Instant endedAt;
    private String status = "RUNNING";
    private String errorMessage;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<SpanEvent> events = new ArrayList<>();

    public SpanRecord(
            String spanId,
            String traceId,
            String parentSpanId,
            String name,
            String kind,
            Map<String, String> attributes
    ) {
        this.spanId = spanId;
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.name = name;
        this.kind = kind;
        this.startedAt = Instant.now();
        this.attributes.putAll(attributes);
    }

    public void addEvent(String name, String detail) {
        events.add(new SpanEvent(Instant.now(), name, detail));
    }

    public void end(String status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
        this.endedAt = Instant.now();
    }

    public long durationMillis() {
        Instant end = endedAt == null ? Instant.now() : endedAt;
        return Duration.between(startedAt, end).toMillis();
    }

    public String spanId() {
        return spanId;
    }

    public String traceId() {
        return traceId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    public String name() {
        return name;
    }

    public String kind() {
        return kind;
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

    public String errorMessage() {
        return errorMessage;
    }

    public Map<String, String> attributes() {
        return Map.copyOf(attributes);
    }

    public List<SpanEvent> events() {
        return List.copyOf(events);
    }
}
