package com.agent.demo11.mcp;

import com.agent.demo11.framework.JsonUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class McpStdioClient {

    private final McpServerConfig serverConfig;

    public McpStdioClient(McpServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public List<McpTool> listTools() {
        try {
            return withProcess((writer, reader) -> {
                initialize(writer, reader);
                send(writer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}");

                send(writer, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
                String response = readResponse(reader);
                System.out.println("\n[调试] mcp 列出工具集合的 response = " + response);

                String result = JsonUtil.objectField(response, "result");
                String toolsArray = JsonUtil.arrayField(result, "tools");

                List<McpTool> tools = new ArrayList<>();
                for (String toolObject : JsonUtil.splitTopLevelObjects(toolsArray)) {
                    tools.add(new McpTool(
                            JsonUtil.stringField(toolObject, "name"),
                            JsonUtil.stringField(toolObject, "description"),
                            JsonUtil.objectField(toolObject, "inputSchema")
                    ));
                }
                return tools;
            });
        } catch (IOException ex) {
            throw new IllegalStateException("读取 MCP 工具列表失败：" + ex.getMessage(), ex);
        }
    }

    public McpCallResult callTool(String toolName, String argumentsJson) {
        String safeArgumentsJson = argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson;
        try {
            return withProcess((writer, reader) -> {
                initialize(writer, reader);
                send(writer, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}");

                String request = "{"
                        + "\"jsonrpc\":\"2.0\","
                        + "\"id\":2,"
                        + "\"method\":\"tools/call\","
                        + "\"params\":{"
                        + "\"name\":\"" + JsonUtil.escape(toolName) + "\","
                        + "\"arguments\":" + safeArgumentsJson
                        + "}"
                        + "}";
                send(writer, request);

                String response = readResponse(reader);
                System.out.println("\n[调试] mcp 工具调用的 response = " + response);

                String result = JsonUtil.objectField(response, "result");
                if (result == null) {
                    String error = JsonUtil.objectField(response, "error");
                    return new McpCallResult(false, error == null ? response : error);
                }

                Boolean isError = JsonUtil.booleanField(result, "isError");
                String content = extractContentText(result);
                return new McpCallResult(!Boolean.TRUE.equals(isError), content);
            });
        } catch (IOException ex) {
            throw new IllegalStateException("调用 MCP 工具失败：" + ex.getMessage(), ex);
        }
    }

    private void initialize(BufferedWriter writer, BufferedReader reader) throws IOException {
        send(writer, "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":1,"
                + "\"method\":\"initialize\","
                + "\"params\":{"
                + "\"protocolVersion\":\"2024-11-05\","
                + "\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"java-demo11\",\"version\":\"1.0\"}"
                + "}"
                + "}");
        readResponse(reader);
    }

    private static String extractContentText(String resultObject) {
        String contentArray = JsonUtil.arrayField(resultObject, "content");
        if (contentArray == null) {
            return "";
        }

        List<String> texts = new ArrayList<>();
        for (String item : JsonUtil.splitTopLevelObjects(contentArray)) {
            String text = JsonUtil.stringField(item, "text");
            texts.add(text == null ? item : text);
        }
        return String.join("\n", texts);
    }

    private <T> T withProcess(ProcessCallback<T> callback) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(serverConfig.command());
        command.addAll(serverConfig.args());

        // process 这个变量就代表那个已经跑起来的子进程，即用当前 JDK 启动一个跑 WeatherMcpServer 的独立 JVM 进程
        Process process = new ProcessBuilder(command)// 准备一个进程构建器，command 是个 List<String>，第一个元素是可执行文件，后面是参数
                .redirectError(ProcessBuilder.Redirect.INHERIT)// 把子进程的 stderr（标准错误）接到父进程的 stderr，这样子进程打印的报错你能直接看到
                .start();// 真正 fork 出子进程，返回 Process 对象，后续可以用它读写子进程的 stdin/stdout

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return callback.execute(writer, reader);
        } finally {
            process.destroy();
            try {
                if (!process.waitFor(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    private static void send(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private static String readResponse(BufferedReader reader) throws IOException {
        String response = reader.readLine();
        if (response == null) {
            throw new IOException("MCP Server 没有返回响应。");
        }
        return response;
    }

    @FunctionalInterface
    private interface ProcessCallback<T> {
        T execute(BufferedWriter writer, BufferedReader reader) throws IOException;
    }
}
