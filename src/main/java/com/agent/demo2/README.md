# Java Demo2：多轮对话与短期记忆

这个目录是 `demo2/memory_demo.py` 的 Java 版本。它在 demo1 的基础上多做了一件关键事情：不再只发送一轮消息，而是把历史 `user` 和 `assistant` 消息保存下来，下一轮继续发给模型。

## 这一节要学什么

所谓“短期记忆”，在这个 demo 里不是数据库，也不是复杂框架，而是一个 `List<ChatMessage>`：

```text
system
user
assistant
user
assistant
...
```

每一轮流程是：

```text
用户输入
 -> append user message
 -> 裁剪最近 MAX_TURNS 轮历史
 -> 把完整 messages 发给模型
 -> 得到 assistant answer
 -> append assistant message
 -> 再裁剪一次
```

## Python 到 Java 的对应关系

| Python | Java |
| --- | --- |
| `messages: list[dict[str, str]]` | `MessageStore` 内部的 `List<ChatMessage>` |
| `create_system_message()` | `createSystemMessage()` |
| `trim_messages(...)` | `MessageStore.trim()` |
| `messages.append(...)` | `messageStore.append(...)` |
| `input(...)` | `BufferedReader.readLine()` |
| `requests.post(...)` | `HttpClient.send(...)` |

## 为什么要裁剪 messages

多轮对话会让请求越来越长：

```text
第 1 轮：system + user
第 2 轮：system + user + assistant + user
第 3 轮：system + user + assistant + user + assistant + user
```

如果一直不裁剪，请求 token 会越来越多，成本和延迟都会上升。所以本 demo 保留：

```text
1 条 system message + 最近 4 轮 user/assistant 对话
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo2
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo2.DeepSeekMemoryDemo
```

可以用这个方式验证记忆：

```text
你：我叫小明
助手：...
你：我叫什么？
```

如果第二轮能回答“小明”，说明模型不是自己记住了你，而是程序把上一轮消息又发给了模型。

## 当前阶段的核心理解

demo1 是“一次调用”。

demo2 是“带历史消息的一次调用反复执行”。

所以 demo2 的本质是：

```text
Agent 的早期记忆 = messages 历史上下文
```
