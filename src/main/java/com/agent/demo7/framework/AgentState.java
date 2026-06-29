package com.agent.demo7.framework;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentState {
    private final String goal;
    private final Map<String, String> sharedContext = new LinkedHashMap<>();
    private String lastToolName;
    private String lastToolResultJson;
    private boolean completed;
    private int loopCount;

    public AgentState(String goal) {
        this.goal = goal;
    }

    public String goal() {
        return goal;
    }

    public Map<String, String> sharedContext() {
        return sharedContext;
    }

    public String lastToolName() {
        return lastToolName;
    }

    public void lastToolName(String lastToolName) {
        this.lastToolName = lastToolName;
    }

    public String lastToolResultJson() {
        return lastToolResultJson;
    }

    public void lastToolResultJson(String lastToolResultJson) {
        this.lastToolResultJson = lastToolResultJson;
    }

    public boolean completed() {
        return completed;
    }

    public void completed(boolean completed) {
        this.completed = completed;
    }

    public int loopCount() {
        return loopCount;
    }

    public void loopCount(int loopCount) {
        this.loopCount = loopCount;
    }
}

