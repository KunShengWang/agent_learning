# Java Demo8：Workflow Agent

这个目录是 `demo8` 的 Java 版本。它把 demo7 的“自由工具调用循环”改成了“固定工作流编排”。

## 这一节要学什么

demo7 的核心是：

```text
LLM 自己决定下一步要调用哪个工具
```

demo8 的核心是：

```text
程序先规定流程节点，LLM 只在部分节点里做判断或生成内容
```

也就是：

```text
classify -> inspect -> plan -> apply -> verify -> report
```

## 和 demo7 的区别

demo7 更像自由 Agent：

```text
模型决定：读文件？搜索？修改？继续？
```

demo8 更像可控 Workflow：

```text
程序决定流程：先分类，再观察，再规划，再执行，再验证，再汇报
```

这类设计适合需要稳定步骤、审计链路、结果验证的任务。

## 关键文件

```text
src/main/java/com/example/agenttutorial/demo8/
  WorkflowDemo.java
  framework/
    Workflow.java
    WorkflowNode.java
    WorkflowContext.java
    LlmClient.java
    JsonUtil.java
  workflow/
    CodeWorkflowContext.java
    ClassifyNode.java
    InspectNode.java
    PlanNode.java
    ApplyNode.java
    VerifyNode.java
    ReportNode.java
  tools/
    WorkspaceTools.java
```

## 示例工作区

被 Agent 操作的示例项目也是 Java：

```text
project_workspace/
  pom.xml
  src/main/java/com/example/workspace/
    Main.java
    AppConfig.java
    GreetingService.java
    DiscountService.java
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo8
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo8.WorkflowDemo
```

可以这样测试：

```text
你：帮我找到 GreetingService 里的 greetUser，并把空名字处理改得更友好。
```

## 本节核心理解

```text
Workflow Agent = 固定流程控制 + 局部 LLM 判断 + 工具执行 + 显式验证
```
