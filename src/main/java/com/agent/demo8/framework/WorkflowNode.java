package com.agent.demo8.framework;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class WorkflowNode {

    private final String name;
    private final Map<String, WorkflowNode> nextNodes = new LinkedHashMap<>();

    protected WorkflowNode(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public abstract String run(WorkflowContext context) throws Exception;

    public WorkflowNode connect(String action, WorkflowNode node) {
        nextNodes.put(action, node);
        return node;
    }

    public WorkflowNode route(String action) {
        return nextNodes.get(action);
    }
}
