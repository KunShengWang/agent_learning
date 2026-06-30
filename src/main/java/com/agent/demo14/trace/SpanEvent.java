package com.agent.demo14.trace;

import java.time.Instant;

public record SpanEvent(
        Instant time,
        String name,
        String detail
) {
}
