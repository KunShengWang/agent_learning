# Demo14: Tracing

这一节学习 Tracing。

核心问题：

```text
Agent 做了一次任务之后，我们如何复盘它每一步做了什么、花了多久、哪里失败、调用了哪些工具？
```

## 本节要学的对象

```text
Trace
```

一次完整 Agent 运行。

```text
Span
```

Trace 里面的一个步骤，例如 agent.run、llm.plan、tool.read_file、tool.replace_text、verify.compile。

```text
SpanEvent
```

Span 内部发生的事件，例如 prompt 准备完成、工具参数生成、工具执行结果返回。

```text
TraceRecorder
```

负责创建 trace、开始 span、结束 span、记录事件。

```text
TraceExporter
```

把 trace 导出为 JSONL 和 Markdown 报告。

```text
TraceAnalyzer
```

从 trace 中分析总耗时、工具调用次数、失败 span、慢 span。

## 和 demo12、demo13 的关系

```text
demo12 Harness
  重点：如何评估 Agent 做得对不对

demo13 Context Engineering
  重点：调用模型前，哪些上下文进入 prompt

demo14 Tracing
  重点：Agent 运行时，每一步如何被记录和复盘
```

## 运行方式

在 IDEA 中运行：

```text
com.agent.demo14.TracingDemo
```

运行后生成：

```text
demo14/runs/latest/trace.jsonl
demo14/runs/latest/trace-report.md
```

## 一句话总结

```text
Tracing = 给 Agent 的一次运行建立可复盘的执行日志和时间线。
```
