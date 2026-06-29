package com.agent.demo9.framework;

public class Workflow {

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

            context.logs().add("step=" + stepIndex + ", node=" + current.name());
            String action = current.run(context);
            context.logs().add("node=" + current.name() + " -> action=" + action);

            if ("done".equals(action)) {
                break;
            }

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
