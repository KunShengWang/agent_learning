# demo16：Spring AI 流式输出 / Flux / SSE

## 学习目标

这个 demo 学习三个概念：

```text
Flux<String>      一段一段返回数据的响应式流
SSE               Server-Sent Events，后端持续推送文本给客户端
ChatModel.stream  Spring AI 的 LLM 流式输出
```

它对应 `super-agent` 里的这类代码：

```java
@PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
public Flux<String> stream(...) {
    return businessChatService.openConversationStream(dto);
}
```

## 文件

```text
src/main/java/com/agent/demo16/Demo16StreamingSseApplication.java
src/main/java/com/agent/demo16/StreamingSseController.java
src/main/java/com/agent/demo16/StreamRequest.java
```

## 运行方式

先设置 API Key：

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
```

启动 demo16：

```powershell
mvn spring-boot:run "-Dspring-boot.run.main-class=com.agent.demo16.Demo16StreamingSseApplication"
```

默认端口：

```text
http://localhost:8016
```

## 接口 1：不调用 LLM 的假流式输出

这个接口用来先看懂 SSE，不需要模型参与。

浏览器访问：

```text
http://localhost:8016/demo16/mock-stream
```

或者 PowerShell：

```powershell
curl.exe -N http://localhost:8016/demo16/mock-stream
```

你会看到内容不是一次性返回，而是大约每 700ms 返回一段。

## 接口 2：调用 LLM 的真实流式输出

```powershell
curl.exe -N -X POST http://localhost:8016/demo16/llm-stream `
  -H "Content-Type: application/json" `
  -d "{\"question\":\"请用三句话解释什么是 Spring AI 的流式输出。\"}"
```

你会看到：

```text
正在调用 LLM 流式接口...

模型生成的第一段
模型生成的第二段
模型生成的第三段
...
[done]
```

## 核心代码

### 1. Controller 返回 Flux

```java
@GetMapping(value = "/mock-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> mockStream() {
    return Flux.interval(Duration.ofMillis(700))
            .take(chunks.size())
            .map(index -> chunks.get(index.intValue()));
}
```

含义：

```text
Flux 不是一个普通 String
Flux 是一个未来会不断吐出 String 的数据流
```

### 2. Spring AI 流式调用

```java
Flux<String> modelChunks = chatModel.stream(prompt)
        .map(this::extractText)
        .filter(text -> !text.isBlank());
```

含义：

```text
chatModel.call(prompt)    一次性返回完整 ChatResponse
chatModel.stream(prompt)  返回 Flux<ChatResponse>，模型边生成边返回
```

### 3. 拼接开始、模型输出、结束

```java
return Flux.concat(
        Flux.just("正在调用 LLM 流式接口...\n\n"),
        modelChunks,
        Flux.just("\n\n[done]\n")
);
```

含义：

```text
先发一个开始提示
再转发模型流式输出
最后发一个结束标记
```

## 和 super-agent 的对应关系

demo16 是最小教学版。

`super-agent` 里对应的是：

```text
BusinessChatController.stream()
-> BusinessChatService.openConversationStream()
-> bindClientChannel()
-> activateGeneration()
-> buildConversationExecution()
-> executor.execute(taskInfo)
-> ObservedChatModelService.streamText()
```

区别是：

```text
demo16 只演示流式输出
super-agent 还要处理会话租约、任务取消、Trace、RAG、工具调用、落库、引用来源
```

## 你要重点观察

1. `mock-stream` 为什么不是一次性返回。
2. `llm-stream` 里模型回复为什么可以边生成边展示。
3. `Flux<String>` 和普通 `String` 的区别。
4. `chatModel.stream(prompt)` 和 `chatModel.call(prompt)` 的区别。
5. 为什么真实 Agent 产品通常都要用流式输出。
