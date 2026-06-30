package com.agent.demo14.trace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TraceExporter {

    public void writeJsonl(TraceRecord trace, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder builder = new StringBuilder();
        builder.append(traceJson(trace)).append("\n");
        for (SpanRecord span : trace.spans()) {
            builder.append(spanJson(span)).append("\n");
            for (SpanEvent event : span.events()) {
                builder.append(eventJson(span, event)).append("\n");
            }
        }
        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    public void writeMarkdown(TraceRecord trace, TraceAnalysis analysis, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder builder = new StringBuilder();
        builder.append("# Demo14 Trace Report\n\n");
        builder.append("- traceId: ").append(trace.traceId()).append("\n");
        builder.append("- status: ").append(trace.status()).append("\n");
        builder.append("- durationMs: ").append(trace.durationMillis()).append("\n");
        builder.append("- spanCount: ").append(trace.spans().size()).append("\n");
        builder.append("- toolCallCount: ").append(analysis.toolCallCount()).append("\n");
        builder.append("- slowestSpan: ").append(analysis.slowestSpanName())
                .append(" (").append(analysis.slowestSpanMillis()).append(" ms)\n\n");

        builder.append("## Goal\n\n");
        builder.append("```text\n").append(trace.goal()).append("\n```\n\n");

        builder.append("## Duration By Kind\n\n");
        builder.append("| Kind | Duration Ms |\n");
        builder.append("| --- | ---: |\n");
        for (Map.Entry<String, Long> entry : analysis.durationByKind().entrySet()) {
            builder.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
        }
        builder.append("\n");

        builder.append("## Timeline\n\n");
        builder.append("| Span | Kind | Parent | Status | Duration Ms |\n");
        builder.append("| --- | --- | --- | --- | ---: |\n");
        for (SpanRecord span : trace.spans()) {
            builder.append("| ")
                    .append(span.name())
                    .append(" | ")
                    .append(span.kind())
                    .append(" | ")
                    .append(span.parentSpanId() == null ? "" : span.parentSpanId())
                    .append(" | ")
                    .append(span.status())
                    .append(" | ")
                    .append(span.durationMillis())
                    .append(" |\n");
        }

        builder.append("\n## Events\n\n");
        for (SpanRecord span : trace.spans()) {
            builder.append("### ").append(span.name()).append("\n\n");
            for (SpanEvent event : span.events()) {
                builder.append("- ").append(event.time())
                        .append(" `").append(event.name()).append("`: ")
                        .append(event.detail().replace("\n", " "))
                        .append("\n");
            }
            builder.append("\n");
        }

        builder.append("## Trace Attributes\n\n");
        for (Map.Entry<String, String> entry : trace.attributes().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private String traceJson(TraceRecord trace) {
        return "{"
                + "\"type\":\"trace\","
                + "\"trace_id\":\"" + escape(trace.traceId()) + "\","
                + "\"name\":\"" + escape(trace.name()) + "\","
                + "\"status\":\"" + escape(trace.status()) + "\","
                + "\"duration_ms\":" + trace.durationMillis()
                + "}";
    }

    private String spanJson(SpanRecord span) {
        return "{"
                + "\"type\":\"span\","
                + "\"trace_id\":\"" + escape(span.traceId()) + "\","
                + "\"span_id\":\"" + escape(span.spanId()) + "\","
                + "\"parent_span_id\":\"" + escape(span.parentSpanId()) + "\","
                + "\"name\":\"" + escape(span.name()) + "\","
                + "\"kind\":\"" + escape(span.kind()) + "\","
                + "\"status\":\"" + escape(span.status()) + "\","
                + "\"duration_ms\":" + span.durationMillis()
                + "}";
    }

    private String eventJson(SpanRecord span, SpanEvent event) {
        return "{"
                + "\"type\":\"event\","
                + "\"trace_id\":\"" + escape(span.traceId()) + "\","
                + "\"span_id\":\"" + escape(span.spanId()) + "\","
                + "\"time\":\"" + escape(event.time().toString()) + "\","
                + "\"name\":\"" + escape(event.name()) + "\","
                + "\"detail\":\"" + escape(event.detail()) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
