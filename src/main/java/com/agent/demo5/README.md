# Java Demo5：ReAct 风格 Agent 循环

这个目录是 `demo5/react_demo.py` 的 Java 版本。它在 demo3 的 Tool Calling 和 demo4 的状态推进之间取了一个折中：不把流程写死成固定 action，但仍然维护结构化状态，让模型根据观察结果继续决定下一步。

## 这一节要学什么

ReAct 可以理解成：

```text
Reason -> Act -> Observe -> Reason -> Act -> Observe -> Final Answer
```

在代码里对应的是：

```text
组装 system + state + messages
 -> 调用模型
 -> 如果模型返回 tool_calls，就执行工具
 -> 把工具结果写回 messages
 -> 同步更新 AgentState
 -> 继续下一轮循环
 -> 如果模型不再调用工具，就输出最终回答
```

## 和 demo4 的区别

demo4 是固定状态机：

```text
decide_path -> draft_content -> create_file -> finish
```

demo5 是自由 ReAct 循环：

```text
模型可以根据上下文自行决定：
创建文件
读取文件
列出文件
继续调用工具
或者直接结束
```

所以 demo5 更灵活，但也更依赖提示词、工具设计和循环次数限制。

## Python 到 Java 的对应关系

| Python | Java |
| --- | --- |
| `run_react_agent(...)` | `runReActAgent(...)` |
| `create_agent_state()` | `AgentState` |
| `update_state_from_tool_result(...)` | `updateStateFromToolResult(...)` |
| `build_runtime_messages(...)` | `buildRuntimeMessages(...)` |
| `build_tools()` | `buildToolsJson()` |
| `execute_tool_call(...)` | `executeToolCall(...)` |

## 本 demo 的工具

Java 版提供了三个工具：

```text
create_text_file
read_text_file
list_files
```

工具操作目录被限制在：

```text
java-demo5/generated_files
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo5
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo5.ReActDemo
```

可以这样测试：

```text
你：帮我生成一份 Java 学习计划，保存成 markdown 文件，然后再读出来检查格式。
```

你应该看到类似：

```text
[循环 1] 模型选择工具：create_text_file
[循环 2] 模型选择工具：read_text_file
助手：...
```

## 当前阶段的核心理解

demo5 要记住一句话：

```text
ReAct 不是固定步骤，而是“观察工具结果后继续决策”的循环。
```

这也是后面 `demo6` 抽象 Agent Runtime 的直接基础。
