# Java Demo6：最小 Agent 框架抽象

这个目录是 `demo6` 的 Java 版本。它不是新增某一个工具能力，而是把 `demo1-demo5` 里反复出现的通用逻辑抽成一个最小框架。

## 这一节要学什么

前面几个 demo 的代码基本都是“写在一个脚本里”。`demo6` 开始拆层：

```text
AgentRuntime：负责主循环
MessageStore：负责历史消息和裁剪
ToolRegistry：负责工具注册、schema 暴露、工具执行
AgentTool：用注解声明工具
LlmClient：负责和 DeepSeek API 通信
BuiltinFileTools：业务工具实现
```

这就是后面 coding agent、workflow、MCP 接入的基础。

## 和 demo5 的区别

demo5 是：

```text
ReAct 主循环 + 工具定义 + 消息管理 + HTTP 调用 全写在一个类里
```

demo6 是：

```text
把这些能力拆成可复用组件
```

所以 demo6 学的是工程结构，不是新花样。

## 主要文件

```text
src/main/java/com/example/agenttutorial/demo6/FrameworkDemo.java
src/main/java/com/example/agenttutorial/demo6/framework/AgentRuntime.java
src/main/java/com/example/agenttutorial/demo6/framework/ToolRegistry.java
src/main/java/com/example/agenttutorial/demo6/framework/MessageStore.java
src/main/java/com/example/agenttutorial/demo6/framework/AgentTool.java
src/main/java/com/example/agenttutorial/demo6/framework/LlmClient.java
src/main/java/com/example/agenttutorial/demo6/tools/BuiltinFileTools.java
```

## Java 版和 Python 版的对应关系

| Python | Java |
| --- | --- |
| `AgentRuntime` | `AgentRuntime` |
| `MessageStore` | `MessageStore` |
| `ToolRegistry` | `ToolRegistry` |
| `@tool` 装饰器 | `@AgentTool` 注解 |
| `ToolDefinition` dataclass | `ToolDefinition` record |
| `create_runtime(...)` | `RuntimeFactory.createRuntime(...)` |
| `builtin_tools.py` | `BuiltinFileTools` |

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo6
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo6.FrameworkDemo
```

可以这样测试：

```text
你：帮我生成一份 Java Agent 学习计划，保存成 markdown 文件，然后再读出来检查格式。
```

## 当前阶段的核心理解

demo6 要记住一句话：

```text
框架化 = 把 ReAct 循环、工具注册、消息存储、模型调用从业务代码里拆出来。
```

后面 `demo7` 的 Coding Agent 会复用这个思路：不重写 Agent 主循环，只换一组更适合代码任务的工具和提示词。
