# demo15：Spring AI 基础模型调用

## 学习目标

这个 demo 学习 Spring AI 最基础的模型调用。

它对应前面的 `demo1`：

```text
demo1   手写 HTTP 请求调用 DeepSeek
demo15  使用 Spring AI 的 ChatModel 调用 DeepSeek
```

## 核心变化

在 `demo1` 中，我们自己做这些事：

```text
拼 JSON 请求体
设置 Authorization 请求头
发送 HTTP 请求
解析 JSON 响应
读取 choices[0].message.content
```

在 `demo15` 中，这些细节交给 Spring AI：

```text
Message / Prompt
-> ChatModel.call()
-> ChatResponse
-> response.getResult().getOutput().getText()
```

也就是说：

```text
Spring AI 不是改变 LLM 调用本质，
而是把 HTTP、JSON、模型配置、响应解析封装成统一的 Java API。
```

## 关键类

```text
Demo15SpringAiBasicApplication
```

重点看：

```java
List<Message> messages = List.of(
        new SystemMessage("..."),
        new UserMessage("...")
);

ChatResponse response = chatModel.call(new Prompt(messages));
String answer = response.getResult().getOutput().getText();
```

## 运行方式

先设置 DeepSeek API Key：

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
```

然后运行：

```powershell
mvn spring-boot:run "-Dspring-boot.run.main-class=com.agent.demo15.Demo15SpringAiBasicApplication"
```

## 你应该学到什么

1. `ChatModel` 是 Spring AI 对聊天模型的统一抽象。
2. `SystemMessage` 和 `UserMessage` 对应底层请求里的 `messages`。
3. `Prompt` 是发送给模型的输入对象。
4. `ChatResponse` 是模型返回结果的统一对象。
5. Spring AI 封装了 HTTP 调用，但没有改变“发送 messages 给模型”的本质。

## 和 super-agent 的对应关系

后续回看 `super-agent` 时，对应位置是：

```text
super-agent-business/super-agent-business-chat/src/main/java/org/javaup/ai/chatagent/service/ObservedChatModelService.java
```

`super-agent` 不是直接在业务里到处调模型，而是把模型调用集中封装到 `ObservedChatModelService`，方便记录耗时、Token、成本和 Trace。
