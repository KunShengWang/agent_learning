# Java Demo11：MCP Agent

这个目录是 `demo11` 的 Java 学习版。它演示如何把一个标准化外部工具服务接入 Agent Runtime。

## 这一节要学什么

前面的 demo 里，工具大多是本地 Java 方法：

```text
Agent Runtime -> ToolRegistry -> Java 方法
```

demo11 学的是 MCP 思想：

```text
Agent Runtime -> MCP Adapter -> MCP Server -> 外部工具
```

也就是说，Agent Runtime 不需要知道天气工具具体怎么实现，只需要通过 adapter 把 MCP Server 暴露的工具转换成 Runtime 认识的 `ToolDefinition`。

## 本节流程

```text
启动本地 Weather MCP Server
        ↓
McpAdapter 读取 server 的 tools/list
        ↓
把 MCP tools 转成 ToolDefinition
        ↓
发送给 LLM
        ↓
LLM 返回 tool_call
        ↓
McpAdapter 调用 server 的 tools/call
        ↓
工具结果作为 tool 消息回传给 LLM
```

## 关键文件

```text
src/main/java/com/example/agenttutorial/demo11/
  McpAgentDemo.java
  framework/
    AgentRuntime.java
    ToolRegistry.java
    LlmClient.java
    ...
  mcp/
    WeatherMcpServer.java
    McpAdapter.java
    McpStdioClient.java
    McpServerConfig.java
```

## 当前天气工具

`WeatherMcpServer` 暴露两个工具：

```text
query_weather(city)
list_supported_cities()
```

Agent 侧看到的工具名会带 server 前缀：

```text
weather_query_weather
weather_list_supported_cities
```

当前支持城市：

```text
北京、上海、杭州、深圳
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo11
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo11.McpAgentDemo
```

可以这样测试：

```text
你：帮我查一下杭州今天的天气，并给我一个出行建议。
你：深圳天气怎么样？
你：支持查哪些城市？
```

## 本节核心理解

```text
MCP 不是让 LLM 直接执行工具，而是把外部工具服务标准化，再由 adapter 接到 Agent Runtime。
```

这个 Java 版本使用纯 JDK 手写了一个最小 stdio MCP 风格 server/client，目的是让你看清楚 adapter 在协议和本地工具注册表之间做了什么转换。
