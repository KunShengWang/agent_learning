package com.agent.demo12.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TraceExporter {

    private TraceExporter() {
    }

    public static void writeJsonl(Path path, List<TraceEvent> events) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder builder = new StringBuilder();
        for (TraceEvent event : events) {
            builder.append("{")
                    .append("\"time\":\"").append(escape(event.time().toString())).append("\",")
                    .append("\"case_id\":\"").append(escape(event.caseId())).append("\",")
                    .append("\"type\":\"").append(escape(event.type())).append("\",")
                    .append("\"name\":\"").append(escape(event.name())).append("\",")
                    .append("\"detail\":\"").append(escape(event.detail())).append("\"")
                    .append("}\n");
        }
        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}

