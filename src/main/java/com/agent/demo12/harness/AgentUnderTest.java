package com.agent.demo12.harness;

import java.io.IOException;

public interface AgentUnderTest {

    AgentRunResult run(AgentTask task, TraceRecorder traceRecorder) throws IOException;
}

