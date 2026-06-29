package com.agent.demo8;

import com.agent.demo8.framework.LlmClient;
import com.agent.demo8.framework.Workflow;
import com.agent.demo8.workflow.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class WorkflowDemo {

    private static final int MAX_WORKFLOW_STEPS = 6;
    private static final Path WORKSPACE_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo8/project_workspace")
            .toAbsolutePath()
            .normalize();

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        LlmClient llmClient = new LlmClient(apiKey);
        Workflow workflow = buildWorkflow(llmClient);

        System.out.println("Workflow Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我找到 GreetingService 里的 greetUser，并把空名字处理改得更友好。");
        System.out.println("工作区目录：" + WORKSPACE_DIR);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("\n你：");
            String goal = reader.readLine();
            if (goal == null) {
                break;
            }

            goal = goal.trim();
            if (goal.isEmpty()) {
                System.out.println("请输入任务目标。");
                continue;
            }

            if ("exit".equalsIgnoreCase(goal) || "quit".equalsIgnoreCase(goal)) {
                System.out.println("对话结束。");
                break;
            }

            CodeWorkflowContext context = new CodeWorkflowContext(goal, WORKSPACE_DIR);
            try {
                workflow.run(context);
            } catch (Exception ex) {
                System.out.println("\n执行失败：" + ex.getMessage());
                continue;
            }

            System.out.println("\n--- Workflow Logs ---");
            for (String log : context.logs()) {
                System.out.println(log);
            }

            System.out.println("\n--- Result ---");
            System.out.println(context.report().isBlank() ? "任务完成。" : context.report());
        }
    }

    private static Workflow buildWorkflow(LlmClient llmClient) {
        ClassifyNode classify = new ClassifyNode("classify", llmClient);
        InspectNode inspect = new InspectNode("inspect");
        PlanNode plan = new PlanNode("plan", llmClient);
        ApplyNode apply = new ApplyNode("apply");
        VerifyNode verify = new VerifyNode("verify");
        ReportNode report = new ReportNode("report", llmClient);

        // 安排下工作节点的执行顺序
        classify.connect("inspect", inspect);
        inspect.connect("plan", plan);
        inspect.connect("report", report);
        plan.connect("apply", apply);
        apply.connect("verify", verify);
        verify.connect("report", report);

        return new Workflow(classify, MAX_WORKFLOW_STEPS);
    }
}
