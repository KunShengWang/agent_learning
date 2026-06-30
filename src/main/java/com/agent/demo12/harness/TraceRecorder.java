package com.agent.demo12.harness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TraceRecorder {

    private final String caseId;// 测试用例的文件名
    private final List<TraceEvent> events = new ArrayList<>();

    public TraceRecorder(String caseId) {
        this.caseId = caseId;
    }

    public void agentStarted(String goal) {
        record("agent", "start", goal);
    }

    public void toolCall(String toolName, String argumentsJson) {
        record("tool_call", toolName, argumentsJson);
    }

    public void toolResult(String toolName, String resultSummary) {
        record("tool_result", toolName, resultSummary);
    }

    public void agentFinal(String finalAnswer) {
        record("agent", "final", finalAnswer);
    }

    public void evaluation(String evaluatorName, String detail) {
        record("eval", evaluatorName, detail);
    }

    public List<TraceEvent> events() {
        return List.copyOf(events);
    }

    private void record(String type, String name, String detail) {
        events.add(new TraceEvent(Instant.now(), caseId, type, name, detail));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TraceRecorder{caseId='").append(caseId)
                .append("', events=").append(events.size()).append("}\n");
        for (int i = 0; i < events.size(); i++) {
            TraceEvent event = events.get(i);
            builder.append("  [").append(i).append("] ")
                    .append(event.type()).append(" | ").append(event.name())
                    .append(" | ").append(event.time())
                    .append("\n        detail=").append(truncate(event.detail(), 200))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "null";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...(" + value.length() + " chars)";
    }
}

