package com.agent.demo9.workflow;

import com.agent.demo9.framework.WorkflowContext;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CodeWorkflowContext extends WorkflowContext {

    private final Path workspaceDir;
    private String intent = "edit";
    private String targetFile;
    private String searchQuery;
    private String searchResultJson = "{}";
    private String fileSnapshotJson = "{}";
    private Map<String, String> patchPlan = new LinkedHashMap<>();
    private String applyResultJson = "{}";
    private String verificationResultJson = "{}";
    private String report = "";

    public CodeWorkflowContext(String goal, Path workspaceDir) {
        super(goal);
        this.workspaceDir = workspaceDir;
    }

    public Path workspaceDir() {
        return workspaceDir;
    }

    public String intent() {
        return intent;
    }

    public void intent(String intent) {
        this.intent = intent;
    }

    public String targetFile() {
        return targetFile;
    }

    public void targetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void searchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String searchResultJson() {
        return searchResultJson;
    }

    public void searchResultJson(String searchResultJson) {
        this.searchResultJson = searchResultJson;
    }

    public String fileSnapshotJson() {
        return fileSnapshotJson;
    }

    public void fileSnapshotJson(String fileSnapshotJson) {
        this.fileSnapshotJson = fileSnapshotJson;
    }

    public Map<String, String> patchPlan() {
        return patchPlan;
    }

    public void patchPlan(Map<String, String> patchPlan) {
        this.patchPlan = patchPlan;
    }

    public String applyResultJson() {
        return applyResultJson;
    }

    public void applyResultJson(String applyResultJson) {
        this.applyResultJson = applyResultJson;
    }

    public String verificationResultJson() {
        return verificationResultJson;
    }

    public void verificationResultJson(String verificationResultJson) {
        this.verificationResultJson = verificationResultJson;
    }

    public String report() {
        return report;
    }

    public void report(String report) {
        this.report = report;
    }
}
