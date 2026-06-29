package com.agent.demo11.framework;

public class AgentState {

    private final String goal;
    private String lastToolName;
    private String lastToolResultJson;
    private int loopCount;

    public AgentState(String goal) {
        this.goal = goal;
    }

    public String goal() {
        return goal;
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

    public int loopCount() {
        return loopCount;
    }

    public void loopCount(int loopCount) {
        this.loopCount = loopCount;
    }
}
