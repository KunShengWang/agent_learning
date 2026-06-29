# Java Demo9：HITL Workflow Agent

这个目录是 `demo9` 的 Java 版本。它在 `demo8` 的固定 workflow 基础上加入了 Human-in-the-Loop，也就是人工确认。

## 这一节要学什么

demo8 的核心是固定流程：

```text
classify -> inspect -> plan -> apply -> verify -> report
```

demo9 在真正写文件前插入人工确认：

```text
classify -> inspect -> plan -> approval -> apply -> verify -> report
```

也就是说：

```text
模型可以生成修改计划，但不能直接落盘。
只有人明确输入 yes，程序才会执行写文件。
```

## 为什么需要 HITL

不是所有 Agent 都应该越自动越好。对代码修改、删除文件、执行命令、发消息、下订单这类有风险的动作，常见做法是：

```text
LLM 负责提出计划
程序负责展示风险和修改内容
人负责批准或拒绝
Runtime 只在批准后执行
```

## 关键文件

```text
src/main/java/com/example/agenttutorial/demo9/
  HitlWorkflowDemo.java
  framework/
    Workflow.java
    WorkflowNode.java
    WorkflowContext.java
    LlmClient.java
    JsonUtil.java
  workflow/
    CodeWorkflowContext.java
    HitlWorkflowContext.java
    ClassifyNode.java
    InspectNode.java
    PlanNode.java
    ApprovalNode.java
    SafeApplyNode.java
    VerifyNode.java
    HitlReportNode.java
  tools/
    WorkspaceTools.java
```

## 示例工作区

被 Agent 操作的示例项目仍然是 Java：

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
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo9
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo9.HitlWorkflowDemo
```

可以这样测试：

```text
你：帮我找到 GreetingService 里的 greetUser，并把空名字处理改得更友好。
```

到审批节点时，终端会展示目标文件、修改理由、旧内容、新内容。输入：

```text
yes
```

才会真正写入文件。其他输入都会取消本次修改。

## 本节核心理解

```text
HITL Workflow = 固定流程 + 修改计划 + 人工确认 + 批准后执行 + 验证报告
```
