package com.agent.demo8.framework;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkflowContext {

    private final String goal;
    private final Map<String, String> shared = new LinkedHashMap<>();
    private final List<String> logs = new ArrayList<>();

    public WorkflowContext(String goal) {
        this.goal = goal;
    }

    public String goal() {
        return goal;
    }

    public Map<String, String> shared() {
        return shared;
    }

    public List<String> logs() {
        return logs;
    }
}
