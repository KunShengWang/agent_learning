# Java Demo7：简化版 Coding Agent

这个目录是 `demo7` 的 Java 版本。它基于 `demo6` 的最小 Agent Runtime，换成了面向代码任务的 system prompt 和工具集。

## 这一节要学什么

Coding Agent 的核心不是“直接改代码”，而是：

```text
先观察 -> 再定位 -> 再读取 -> 再修改 -> 最后汇报
```

## 和 demo6 的关系

`demo6` 抽出了通用框架：

```text
AgentRuntime
MessageStore
ToolRegistry
@AgentTool
LlmClient
```

`demo7` 复用这个框架，只做两类变化：

```text
CodingAgentRuntime：更换 system prompt 和 runtime state
CodingTools：提供代码观察和修改工具
```

## 工具分类

观察型工具：

```text
list_files
search_text
search_files_by_name
read_text_file
```

修改型工具：

```text
replace_text_in_file
write_text_file
```

其中 `replace_text_in_file` 要求传入 `expected_occurrences`，目的是防止模型误替换多个位置。

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo7
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo7.CodingAgentDemo
```

可以这样测试：

```text
你：帮我找到 greet_user 的实现，并给空名字加一个更友好的处理。
```

当前阶段的核心理解：

```text
Coding Agent = 通用 Agent Runtime + 面向代码的工具集 + 先观察后修改的约束。
```
