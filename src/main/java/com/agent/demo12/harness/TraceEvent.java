package com.agent.demo12.harness;

import java.time.Instant;

public record TraceEvent(
        Instant time,
        String caseId,
        String type,
        String name,
        String detail
) {
}

