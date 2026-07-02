# demo17：Spring AI Tool Calling

这个 demo 学习 Spring AI 对工具调用的封装。

它对应前面手写的 `demo3 Tool Calling`：

```text
demo3 手写版：
Java 把工具 JSON schema 发给 LLM
-> LLM 返回 tool_call
-> Java 根据 tool name 和 arguments 调用本地方法
-> Java 把工具结果再发给 LLM
-> LLM 生成最终回答

demo17 Spring AI 版：
Java 写普通方法，并用 @Tool 标记
-> ChatClient 把工具说明发给 LLM
-> LLM 决定是否调用工具
-> ChatClient 自动执行 Java 工具方法
-> ChatClient 自动把工具结果交回 LLM
-> LLM 生成最终回答
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent_learning
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn spring-boot:run "-Dspring-boot.run.main-class=com.agent.demo17.Demo17ToolCallingApplication"
```

## 重点观察

运行时重点看控制台有没有这类日志：

```text
[Tool Executed] queryTicketStatus(ticketId=T-1001)
[Tool Executed] createTicketDraft(title=支付服务无法登录, priority=高)
```

看到这些日志，说明模型不是自己编答案，而是先让 Spring AI 调用了 Java 工具方法。

## 你需要掌握什么

1. `@Tool`：把一个 Java 方法暴露成模型可调用工具。
2. `@ToolParam`：描述工具参数，帮助模型正确生成参数。
3. `ChatClient.tools(...)`：把工具注册到本次模型调用里。
4. `ChatClient` 会自动处理工具调用循环，不需要你手动写 while 循环。

## 和 Agent 思想的关系

工具调用是 Agent 从“只会聊天”变成“能做事”的关键。

```text
LLM 负责判断要不要调用工具
Java 程序负责真正执行工具
Spring AI 负责把这套交互流程封装起来
```

后面回到 `super-agent` 时，可以重点看它是如何把搜索、RAG、业务查询等能力包装成工具或执行器的。

## 回到 super-agent 怎么看

学完这个 demo 后，对照 `super-agent` 里的这几类代码：

```text
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/config/ChatAgentConfiguration.java
```

看这里如何把一个普通 Java 方法包装成 `ToolCallback`：

```java
FunctionToolCallback
    .builder("tavily_search", tavilySearchTool::search)
    .description("联网搜索最新信息、事实资料和网页来源。")
    .inputType(TavilySearchRequest.class)
    .build();
```

再看这里如何把工具交给真实 Agent：

```java
ReactAgent.builder()
    .model(chatModel)
    .tools(tavilySearchToolCallback)
    .build();
```

继续对照：

```text
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/tool/TavilySearchTool.java
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/tool/TavilySearchRequest.java
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/tool/TavilySearchToolResult.java
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/rag/executor/ReactAgentExecutor.java
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/support/TavilyToolInputFallbackInterceptor.java
```

你可以这样理解：

```text
demo17:
@Tool 标记方法
-> ChatClient.tools(...)
-> 适合学习工具调用最小闭环

super-agent:
FunctionToolCallback 包装工具
-> ReactAgent.tools(...)
-> Hook 限制模型/工具调用次数
-> Interceptor 处理工具参数、重试、异常
-> Trace/Debug 记录工具调用过程
-> 适合真实工程
```
