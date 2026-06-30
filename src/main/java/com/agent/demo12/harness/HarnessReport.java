package com.agent.demo12.harness;

import java.nio.file.Path;
import java.util.List;

public record HarnessReport(
        List<CaseReport> caseReports,
        Path runRoot
) {
    public boolean passed() {
        return caseReports.stream().allMatch(CaseReport::passed);
    }

    public long passedCaseCount() {
        return caseReports.stream().filter(CaseReport::passed).count();
    }

    public long totalAssertionCount() {
        return caseReports.stream().mapToLong(report -> report.assertions().size()).sum();
    }

    public long passedAssertionCount() {
        return caseReports.stream().mapToLong(CaseReport::passedAssertionCount).sum();
    }

    public String shortSummary() {
        return "Harness 运行完成: cases " + passedCaseCount() + "/" + caseReports.size()
                + ", assertions " + passedAssertionCount() + "/" + totalAssertionCount()
                + ", status=" + (passed() ? "PASS" : "FAIL");
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Harness Report\n\n");
        builder.append("- status: ").append(passed() ? "PASS" : "FAIL").append("\n");
        builder.append("- cases: ").append(passedCaseCount()).append("/").append(caseReports.size()).append("\n");
        builder.append("- assertions: ").append(passedAssertionCount()).append("/").append(totalAssertionCount()).append("\n\n");

        for (CaseReport report : caseReports) {
            builder.append("## ").append(report.caseId()).append("\n\n");
            builder.append("- status: ").append(report.passed() ? "PASS" : "FAIL").append("\n");
            builder.append("- goal: ").append(report.goal()).append("\n");
            builder.append("- final_answer: ").append(report.finalAnswer()).append("\n");
            builder.append("- trace: ").append(report.caseRunDir().resolve("trace.jsonl")).append("\n\n");
            builder.append("| Check | Result | Message |\n");
            builder.append("| --- | --- | --- |\n");
            for (AssertionResult assertion : report.assertions()) {
                builder.append("| ")
                        .append(assertion.name())
                        .append(" | ")
                        .append(assertion.passed() ? "PASS" : "FAIL")
                        .append(" | ")
                        .append(assertion.message().replace("|", "\\|"))
                        .append(" |\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}

