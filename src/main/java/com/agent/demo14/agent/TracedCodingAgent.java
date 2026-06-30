package com.agent.demo14.agent;

import com.agent.demo14.tools.WorkspaceTools;
import com.agent.demo14.trace.SpanHandle;
import com.agent.demo14.trace.TraceRecorder;

import java.nio.file.Path;
import java.util.Map;

public class TracedCodingAgent {

    private final Path workspaceDir;

    public TracedCodingAgent(Path workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public String run(String goal, TraceRecorder recorder, String traceId) throws Exception {
        SpanHandle agentSpan = recorder.startSpan(
                traceId,
                null,
                "agent.run",
                "agent",
                Map.of("goal_length", String.valueOf(goal.length()))
        );

        try {
            recorder.addEvent(agentSpan.spanId(), "agent.started", "Agent 开始处理任务");

            PlanDecision decision = plan(goal, recorder, traceId, agentSpan.spanId());
            recorder.addEvent(agentSpan.spanId(), "agent.plan_ready", "next_action=" + decision.nextAction());

            String oldContent = readGreetingService(recorder, traceId, agentSpan.spanId());
            replaceGreetingService(recorder, traceId, agentSpan.spanId(), oldContent);
            verifyCompile(recorder, traceId, agentSpan.spanId());

            String finalAnswer = "已完成中文问候修改，并通过编译验证。";
            recorder.addEvent(agentSpan.spanId(), "agent.final_answer", finalAnswer);
            recorder.endSpan(agentSpan.spanId(), "OK", null);
            return finalAnswer;
        } catch (Exception ex) {
            recorder.addEvent(agentSpan.spanId(), "agent.error", ex.getMessage());
            recorder.endSpan(agentSpan.spanId(), "ERROR", ex.getMessage());
            throw ex;
        }
    }

    private PlanDecision plan(String goal, TraceRecorder recorder, String traceId, String parentSpanId) {
        SpanHandle span = recorder.startSpan(
                traceId,
                parentSpanId,
                "llm.plan",
                "llm",
                Map.of("mock", "true")
        );
        try {
            String prompt = "根据用户目标决定下一步工具调用: " + goal;
            recorder.addEvent(span.spanId(), "prompt.created", prompt);
            recorder.addEvent(span.spanId(), "model.output", "先读文件，再精确替换，最后编译验证");
            recorder.endSpan(span.spanId(), "OK", null);
            return new PlanDecision("read_then_replace_then_verify");
        } catch (RuntimeException ex) {
            recorder.endSpan(span.spanId(), "ERROR", ex.getMessage());
            throw ex;
        }
    }

    private String readGreetingService(TraceRecorder recorder, String traceId, String parentSpanId) throws Exception {
        SpanHandle span = recorder.startSpan(
                traceId,
                parentSpanId,
                "tool.read_text_file",
                "tool",
                Map.of("tool_name", "read_text_file")
        );
        try {
            String relativePath = "src/main/java/com/example/workspace/GreetingService.java";
            recorder.addEvent(span.spanId(), "tool.arguments", "{\"relative_path\":\"" + relativePath + "\"}");
            String content = WorkspaceTools.readTextFile(workspaceDir, relativePath);
            recorder.addEvent(span.spanId(), "tool.result", "characters_read=" + content.length());
            recorder.endSpan(span.spanId(), "OK", null);
            return content;
        } catch (Exception ex) {
            recorder.endSpan(span.spanId(), "ERROR", ex.getMessage());
            throw ex;
        }
    }

    private void replaceGreetingService(
            TraceRecorder recorder,
            String traceId,
            String parentSpanId,
            String oldContent
    ) throws Exception {
        SpanHandle span = recorder.startSpan(
                traceId,
                parentSpanId,
                "tool.replace_text",
                "tool",
                Map.of("tool_name", "replace_text_in_file")
        );
        try {
            String relativePath = "src/main/java/com/example/workspace/GreetingService.java";
            String oldText = """
                        public String greetUser(String name) {
                            if (name == null || name.isBlank()) {
                                return "Hello, friend!";
                            }
                            return "Hello, " + name.trim() + "!";
                        }
                    """;
            String newText = """
                        public String greetUser(String name) {
                            String trimmedName = name == null ? "" : name.trim();
                            if (trimmedName.isBlank()) {
                                return "你好，朋友！";
                            }
                            return "你好，" + trimmedName + "！";
                        }
                    """;
            recorder.addEvent(span.spanId(), "tool.arguments", "{\"relative_path\":\"" + relativePath + "\"}");
            recorder.addEvent(span.spanId(), "tool.precondition", "old_file_characters=" + oldContent.length());
            String result = WorkspaceTools.replaceTextInFile(workspaceDir, relativePath, oldText, newText);
            recorder.addEvent(span.spanId(), "tool.result", result);
            recorder.endSpan(span.spanId(), "OK", null);
        } catch (Exception ex) {
            recorder.endSpan(span.spanId(), "ERROR", ex.getMessage());
            throw ex;
        }
    }

    private void verifyCompile(TraceRecorder recorder, String traceId, String parentSpanId) throws Exception {
        SpanHandle span = recorder.startSpan(
                traceId,
                parentSpanId,
                "verify.compile",
                "verification",
                Map.of("command", "mvn -q -DskipTests compile")
        );
        try {
            recorder.addEvent(span.spanId(), "verify.started", "开始编译验证");
            String result = WorkspaceTools.compileWithMaven(workspaceDir);
            recorder.addEvent(span.spanId(), "verify.result", result);
            recorder.endSpan(span.spanId(), "OK", null);
        } catch (Exception ex) {
            recorder.endSpan(span.spanId(), "ERROR", ex.getMessage());
            throw ex;
        }
    }
}
