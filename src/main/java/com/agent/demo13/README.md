# Demo13: Context Engineering

这一节学习 Context Engineering。

核心问题：

```text
每次调用 LLM 前，应该把哪些信息放进 messages？
哪些信息只能留在本地？
哪些历史要压缩？
哪些工具结果要裁剪？
哪些工具可以暴露给模型？
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent_learning
javac -encoding UTF-8 -d %TEMP%\agent_learning_demo13_classes (demo13 下所有 java 文件)
java -cp %TEMP%\agent_learning_demo13_classes com.agent.demo13.ContextEngineeringDemo
```

如果在 IDEA 中运行，直接运行：

```text
com.agent.demo13.ContextEngineeringDemo
```

## 目录

```text
demo13/
  memory/long_term_memory.txt         长期记忆样例
  docs/context-engineering-notes.md   工具读取到的资料样例
  runs/latest/context-report.md       运行后生成

src/main/java/com/agent/demo13/
  ContextEngineeringDemo.java         入口
  context/                            上下文工程核心类
  llm/MockLlmClient.java              模拟 LLM，只展示模型看到的上下文
```

## 本节重点

```text
RunContext
```

本地运行上下文。里面可以有用户 ID、权限、密钥、运行目录等。不是所有内容都应该发送给 LLM。

```text
ContextBuilder
```

根据任务、记忆、历史、工具结果和预算，组装真正发送给 LLM 的 messages。

```text
ContextPolicy
```

上下文策略。控制最多保留几条历史、最多放几条记忆、工具结果最多多少字符、prompt 总预算多少。

```text
ContextDecision
```

记录每条上下文为什么被保留、压缩、裁剪或丢弃。

## 一句话总结

```text
Context Engineering = 在调用模型前，工程化地选择、压缩、隔离和组织上下文。
```
