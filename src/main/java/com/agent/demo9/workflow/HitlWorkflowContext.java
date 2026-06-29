package com.agent.demo9.workflow;

import java.nio.file.Path;

public class HitlWorkflowContext extends CodeWorkflowContext {

    private boolean approvalRequired = true;
    private boolean approved = false;
    private boolean rejected = false;
    private String approvalNote = "";
    private String humanFeedback = "";
    private String beforeSnapshotJson = "{}";

    public HitlWorkflowContext(String goal, Path workspaceDir) {
        super(goal, workspaceDir);
    }

    public boolean approvalRequired() {
        return approvalRequired;
    }

    public void approvalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }

    public boolean approved() {
        return approved;
    }

    public void approved(boolean approved) {
        this.approved = approved;
    }

    public boolean rejected() {
        return rejected;
    }

    public void rejected(boolean rejected) {
        this.rejected = rejected;
    }

    public String approvalNote() {
        return approvalNote;
    }

    public void approvalNote(String approvalNote) {
        this.approvalNote = approvalNote;
    }

    public String humanFeedback() {
        return humanFeedback;
    }

    public void humanFeedback(String humanFeedback) {
        this.humanFeedback = humanFeedback;
    }

    public String beforeSnapshotJson() {
        return beforeSnapshotJson;
    }

    public void beforeSnapshotJson(String beforeSnapshotJson) {
        this.beforeSnapshotJson = beforeSnapshotJson;
    }
}
