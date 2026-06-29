package com.agent.demo9;

import com.agent.demo9.framework.LlmClient;
import com.agent.demo9.framework.Workflow;
import com.agent.demo9.workflow.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class HitlWorkflowDemo {

    private static final int MAX_WORKFLOW_STEPS = 7;
    private static final Path WORKSPACE_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo9/project_workspace")
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

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        LlmClient llmClient = new LlmClient(apiKey);
        Workflow workflow = buildWorkflow(llmClient, reader);

        System.out.println("HITL Workflow Demo 已启动。输入 exit 或 quit 结束。");
        System.out.println("你可以试试：帮我找到 GreetingService 里的 greetUser，并把空名字处理改得更友好。");
        System.out.println("工作区目录：" + WORKSPACE_DIR);

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

            HitlWorkflowContext context = new HitlWorkflowContext(goal, WORKSPACE_DIR);
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

    private static Workflow buildWorkflow(LlmClient llmClient, BufferedReader reader) {
        ClassifyNode classify = new ClassifyNode("classify", llmClient);
        InspectNode inspect = new InspectNode("inspect");
        PlanNode plan = new PlanNode("plan", llmClient);
        ApprovalNode approval = new ApprovalNode("approval", reader);
        SafeApplyNode apply = new SafeApplyNode("apply");
        VerifyNode verify = new VerifyNode("verify");
        HitlReportNode report = new HitlReportNode("report", llmClient);

        classify.connect("inspect", inspect);
        inspect.connect("plan", plan);
        inspect.connect("report", report);
        plan.connect("approval", approval);
        approval.connect("approved", apply);
        approval.connect("rejected", report);
        apply.connect("verify", verify);
        verify.connect("report", report);

        return new Workflow(classify, MAX_WORKFLOW_STEPS);
    }
}
