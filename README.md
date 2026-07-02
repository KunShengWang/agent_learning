# Agent Learning

这是一个从 0 手搓 Agent 的 Java 学习仓库。

项目目标不是直接套用成熟框架，而是先用 Java 把 Agent 的底层思想拆开学习：LLM 调用、消息记忆、工具调用、规划、ReAct、Workflow、RAG、MCP、Harness、Context Engineering、Tracing。

学完这些 demo 后，再看 Spring AI、LangChain4j、OpenAI Agents SDK、LangGraph、AutoGen、CrewAI 等框架时，就能理解它们封装的执行步骤和工程边界。

当前进入第二阶段：通过小 demo 学习 Spring AI / Agent 框架知识点，再回到 `super-agent` 对照真实工程实现，最后二开成简历项目。

## 适合谁

- 想从 0 理解 Agent 工作原理的人
- 会 Java，但不想一开始就被框架 API 淹没的人
- 想理解 tool calling、ReAct、RAG、MCP、Tracing、Eval 等概念的人
- 后续准备学习 Spring AI / LangChain4j / OpenAI Agents SDK 的人

## 环境要求

```text
JDK 17+
Maven 3.8+
IDEA 或其他 Java IDE
```

部分早期 demo 会调用大模型 API，需要配置自己的 API Key，例如：

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
```

后面的学习型 demo，例如 `demo12`、`demo13`、`demo14`，默认使用模拟 Agent / Mock LLM，不需要 API Key。

## 项目结构

```text
agent_learning/
  pom.xml
  src/main/java/com/agent/
    demo1/      最小 LLM 调用
    demo2/      多轮对话与短期记忆
    demo3/      Tool Calling
    demo4/      Planning
    demo5/      ReAct
    demo6/      最小 Agent 框架
    demo7/      Coding Agent
    demo8/      Workflow Agent
    demo9/      HITL Workflow
    demo10/     RAG
    demo11/     MCP
    demo12/     Harness
    demo13/     Context Engineering
    demo14/     Tracing
    demo15/     Spring AI 基础模型调用
    demo16/     Spring AI 流式输出 / Flux / SSE
    demo17/     Spring AI Tool Calling

  demo12/       Harness 的测试用例、fixture、运行报告
  demo13/       Context Engineering 的 memory、docs、运行报告
  demo14/       Tracing 的任务、workspace、运行报告
```

注意：

```text
src/main/java/com/agent/demoN   放 Java 源码
demo12 / demo13 / demo14        放运行数据、样例工作区、报告
```

不要把 `runs/`、`target/`、临时 workspace 当成源码提交。

## Demo 导航

| Demo | 主题 | 核心思想 |
| --- | --- | --- |
| demo1 | LLM 调用 | 大模型调用本质是一次 HTTP 请求，发送 `model + messages + 参数`，接收模型回复 |
| demo2 | Memory | 多轮对话不是模型真的记住了，而是程序维护历史 `messages` |
| demo3 | Tool Calling | 模型返回要调用的工具和参数，Java 程序负责真正执行工具 |
| demo4 | Planning | 复杂任务拆成多个步骤，程序维护状态，模型决定下一步 |
| demo5 | ReAct | `思考 -> 调工具 -> 观察结果 -> 再思考` 的循环 |
| demo6 | Agent Framework | 把消息、工具、运行循环抽象成最小框架 |
| demo7 | Coding Agent | Agent 应用到代码场景：先观察、再定位、再修改、再验证 |
| demo8 | Workflow Agent | 固定流程控制：`classify -> inspect -> plan -> apply -> verify -> report` |
| demo9 | HITL | 高风险操作前加入人工审批 |
| demo10 | RAG | 加载、切分、向量化、检索、拼 prompt、回答 |
| demo11 | MCP | 通过 MCP 接入外部工具服务 |
| demo12 | Harness | Agent 的测试台：运行、记录、评估、生成报告 |
| demo13 | Context Engineering | 调用 LLM 前决定哪些上下文进入 prompt |
| demo14 | Tracing | 记录 Agent 执行过程中的 trace、span、event |
| demo15 | Spring AI Basic | 使用 Spring AI `ChatModel` 替代手写 HTTP 调用模型 |
| demo16 | Streaming / SSE | 使用 `Flux<String>` 和 `ChatModel.stream()` 学习流式输出 |
| demo17 | Spring AI Tool Calling | 使用 `ChatClient` 和 `@Tool` 把 Java 方法暴露给模型调用 |

## 学习路线

建议按顺序学习：

```text
demo1  LLM 调用
  -> demo2  Memory
  -> demo3  Tool Calling
  -> demo4  Planning
  -> demo5  ReAct
  -> demo6  Agent Framework
  -> demo7  Coding Agent
  -> demo8  Workflow
  -> demo9  HITL
  -> demo10 RAG
  -> demo11 MCP
  -> demo12 Harness
  -> demo13 Context Engineering
  -> demo14 Tracing
  -> demo15 Spring AI 基础模型调用
  -> demo16 Spring AI 流式输出 / Flux / SSE
  -> demo17 Spring AI Tool Calling
```

框架阶段采用 A/B/C 学习法：

```text
A. 在 agent_learning 写最小框架 demo
B. 回到 super-agent 对照真实工程写法
C. 把能力二开增强到 super-agent，服务简历和面试
```

整体理解：

```text
LLM 调用只是起点。
Agent = LLM + 状态 + 工具 + 规划 + 执行循环 + 上下文管理 + 观测评估。
```

## 关键概念总结

### LLM 与 Messages

大模型不是直接“记住”对话，而是每次根据传入的 `messages` 生成回答。

```text
system message  定义模型角色和规则
user message    用户输入
assistant       模型历史回复
tool            工具执行结果
```

### Tool Calling

模型不会真正执行工具。

真实流程是：

```text
Java 把工具 schema 发给 LLM
LLM 返回 tool_call
Java 根据 tool name 调用真实方法
Java 把工具结果再发回 LLM
LLM 继续判断下一步
```

### ReAct

ReAct 是 Agent 的经典循环：

```text
Reason  思考下一步
Act     调用工具
Observe 观察工具结果
Repeat  继续循环
```

### Workflow

Workflow 适合固定流程、强控制、需要审计的任务。

相比自由 ReAct：

```text
ReAct    更自由，模型决定下一步
Workflow 更稳定，程序规定执行节点
```

### RAG

RAG 让 Agent 回答前先查资料：

```text
加载文档 -> 切分 chunk -> 向量化 -> 存储 -> 检索 -> 拼 prompt -> 回答
```

### MCP

MCP 是外部工具接入协议。

它解决的问题是：

```text
工具如何被发现
工具 schema 如何描述
工具调用如何标准化
工具结果如何返回
```

### Harness

Harness 是 Agent 的测试台。

它负责：

```text
准备测试任务
准备临时工作区
运行 Agent
记录执行过程
执行评估器
生成报告
```

### Context Engineering

Context Engineering 不是简单拼 prompt，而是决定：

```text
哪些 memory 进入 prompt
哪些 history 要压缩
哪些 tool result 要裁剪
哪些本地状态不能给模型看
哪些工具本轮暴露给模型
```

### Tracing

Tracing 记录 Agent 实际发生了什么。

```text
trace  一次完整运行
span   运行中的一个步骤
event  span 内部的关键事件
```

它主要用于复盘、排查、性能分析和后续评估。

## demo12 / demo13 / demo14 的关系

这三个 demo 是后续框架学习很重要的工程基础：

```text
demo12 Harness
  执行后评估：Agent 做得对不对？

demo13 Context Engineering
  执行前组织：LLM 应该看到什么？

demo14 Tracing
  执行中记录：Agent 实际做了什么？
```

完整链路：

```text
用户任务
  -> Context Engineering 组装上下文
  -> Agent / LLM 执行
  -> Tracing 记录过程
  -> Harness 评估结果
  -> 报告 / 改进 / 回归测试
```

## 运行示例

在 IDEA 中可以直接运行对应入口类。

例如：

```text
com.agent.demo12.HarnessDemo
com.agent.demo13.ContextEngineeringDemo
com.agent.demo14.TracingDemo
```

也可以用命令行临时编译某个 demo，例如 demo14：

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent_learning

$out = Join-Path $env:TEMP 'agent_learning_demo14_classes'
if (Test-Path $out) { Remove-Item -LiteralPath $out -Recurse -Force }
New-Item -ItemType Directory -Force $out | Out-Null

$files = Get-ChildItem -Recurse -Filter *.java src\main\java\com\agent\demo14 | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d $out $files
java -cp $out com.agent.demo14.TracingDemo
```

## 后续框架学习对应关系

| 本仓库概念 | 框架里常见名字 |
| --- | --- |
| LLM 调用 | `ChatClient`、`ModelClient`、`Responses API` |
| Memory | `ChatMemory`、`MessageStore`、`ConversationBuffer` |
| Tool Calling | `ToolCallback`、`FunctionTool`、`@Tool` |
| ReAct | Agent loop、tool-use loop |
| Workflow | Graph、Node、Edge、StateGraph |
| HITL | Interrupt、Approval、Human-in-the-loop |
| RAG | `EmbeddingModel`、`VectorStore`、`Retriever` |
| MCP | MCP Server、MCP Client、Tool Adapter |
| Harness | Eval Runner、Benchmark、Experiment |
| Context Engineering | Context Management、Compaction、Prompt Assembly |
| Tracing | Trace、Span、Observability、LangSmith、OpenAI Tracing |

## 学习建议

每学完一个 demo，建议回答三个问题：

```text
1. 这个 demo 比上一个 demo 多了什么能力？
2. 这个能力靠哪些数据结构和模块实现？
3. 如果换成成熟框架，它会被封装成哪个 API？
```

不要只看代码能不能跑，要重点理解每个 demo 背后的 Agent 工程思想。
