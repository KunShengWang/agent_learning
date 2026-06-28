# Java Demo4：显式规划与状态推进

这个目录是 `demo4/planning_demo.py` 的 Java 版本。它和 demo3 最大的区别是：不再使用 `tools/tool_calls` 协议，而是让模型每次只返回一个 JSON 决策，程序根据 `action` 推进状态。

## 这一节要学什么

demo4 的核心流程是：

```text
用户目标
 -> 程序维护 TaskState
 -> 把当前 state 和 stepLogs 发给模型
 -> 模型只返回 JSON 决策
 -> 程序读取 action
 -> 程序执行对应分支
 -> 更新 state
 -> 继续下一步
```

模型只能选择四个动作：

```text
decide_path
draft_content
create_file
finish
```

## 和 demo3 的区别

demo3 是：

```text
模型通过 tool_calls 自由请求工具
```

demo4 是：

```text
模型只负责选择下一步 action
程序按固定状态机执行
```

所以 demo4 的可控性更强。模型不能随便发起任意工具调用，它只能在你规定好的动作集合里选择。

## Python 到 Java 的对应关系

| Python | Java |
| --- | --- |
| `state: dict[str, Any]` | `TaskState` |
| `decision: dict[str, Any]` | `PlanningDecision` |
| `build_state_message(...)` | `buildStateMessage(...)` |
| `call_planner(...)` | `callPlanner(...)` |
| `apply_model_updates(...)` | `applyModelUpdates(...)` |
| `run_task_agent(...)` | `runTaskAgent(...)` |
| `create_text_file(...)` | `createTextFile(...)` |

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo4
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo4.PlanningDemo
```

可以这样测试：

```text
你：帮我生成一份 Java Agent 学习计划，并保存成 markdown 文件。
```

你应该看到类似流程：

```text
[步骤 1] decide_path
[步骤 2] draft_content
[步骤 3] create_file
[工具结果] {"ok":true,...}
[步骤 4] finish
```

## 当前阶段的核心理解

demo4 要记住一句话：

```text
模型负责决策，程序负责状态和执行。
```

这比 demo3 更像一个受控工作流。模型不是直接“想调什么工具就调什么工具”，而是在程序定义好的动作集合中做下一步选择。
