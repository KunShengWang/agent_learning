package com.agent.demo12.harness;

public record AssertionResult(
        String name,
        boolean passed,
        String message
) {
    public static AssertionResult pass(String name, String message) {
        return new AssertionResult(name, true, message);
    }

    public static AssertionResult fail(String name, String message) {
        return new AssertionResult(name, false, message);
    }
}

