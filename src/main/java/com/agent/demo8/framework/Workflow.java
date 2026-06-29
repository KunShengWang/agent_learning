package com.agent.demo8.framework;

public class Workflow {

    // 开始的工作流节点
    private final WorkflowNode startNode;
    private final int maxSteps;

    public Workflow(WorkflowNode startNode, int maxSteps) {
        this.startNode = startNode;
        this.maxSteps = maxSteps;
    }

    public WorkflowContext run(WorkflowContext context) throws Exception {
        WorkflowNode current = startNode;
        for (int stepIndex = 1; stepIndex <= maxSteps; stepIndex++) {
            if (current == null) {
                break;
            }
            // 在上下文中记录执行的步骤和工作流节点名称
            context.logs().add("step=" + stepIndex + ", node=" + current.name());
            // 当前工作流节点执行并返回下一个工作流节点的名称
            String action = current.run(context);
            context.logs().add("node=" + current.name() + " -> action=" + action);

            if ("done".equals(action)) {
                break;
            }
            // 获取下一个执行的工作流节点
            WorkflowNode next = current.route(action);
            if (next == null) {
                context.logs().add("no route for action=" + action + ", stop workflow");
                break;
            }
            current = next;
        }
        return context;
    }
}
