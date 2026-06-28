# Java Demo3：Tool Calling 与文件工具

这个目录是 `demo3/tool_demo.py` 的 Java 版本。它在 demo2 的多轮消息基础上，新增了 Agent 最关键的一步：模型可以请求调用工具，程序在本地执行工具，再把工具结果回灌给模型。

## 这一节要学什么

demo3 的核心闭环是：

```text
用户输入
 -> messages 加入 user
 -> 请求体带上 tools schema
 -> 模型决定是否返回 tool_calls
 -> Java 解析 tool_calls
 -> Java 执行本地工具 create_text_file
 -> 把工具结果作为 role=tool 消息写回 messages
 -> 再请求模型生成最终回答
```

到这一节，程序第一次不只是“问模型要文本”，而是允许模型选择一个受控动作。

## Python 到 Java 的对应关系

| Python | Java |
| --- | --- |
| `build_tools()` | `buildToolsJson()` |
| `tool_call["function"]["arguments"]` | `ToolCall.argumentsJson()` |
| `execute_tool_call(...)` | `executeToolCall(...)` |
| `create_text_file(...)` | `createTextFile(...)` |
| `resolve_safe_path(...)` | `resolveSafePath(...)` |
| `messages.append({"role": "tool", ...})` | `messages.add(ChatMessage.tool(...))` |

## 本 demo 暴露给模型的工具

只有一个工具：

```text
create_text_file
```

参数是：

```json
{
  "relative_path": "notes/agent.md",
  "content": "文件完整内容",
  "overwrite": true
}
```

工具只能写入：

```text
java-demo3/generated_files
```

这是 Agent 工具设计里的第一个安全边界：模型可以提出动作，但程序必须限制动作范围。

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo3
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo3.ToolCallingDemo
```

可以这样测试：

```text
你：帮我创建一个 markdown 文件，内容是一个 Agent 学习大纲。
```

正常情况下，你会看到：

```text
[工具调用] create_text_file
[工具参数] ...
[工具结果] {"ok":true,...}
助手：...
```

## 当前阶段的核心理解

demo1：模型调用。

demo2：把历史 messages 作为短期记忆。

demo3：给模型一个“可执行动作集合”，模型通过 `tool_calls` 表达动作意图，真正执行动作的是 Java 程序。

所以这一节要记住：

```text
模型负责决策，程序负责执行。
```

Agent 不是让模型直接操作系统，而是让模型在你定义好的工具边界内提出调用请求。
