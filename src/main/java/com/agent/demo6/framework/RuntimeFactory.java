package com.agent.demo6.framework;

public final class RuntimeFactory {

    private static final int DEFAULT_MAX_LOOPS = 8;

    private RuntimeFactory() {}

    public static RuntimeBundle createRuntime(String apiKey, int maxTurns, Class<?>... toolClasses) {
        ToolRegistry registry = new ToolRegistry();
        for (Class<?> toolClass : toolClasses) {
            registry.registerToolsFromClass(toolClass);
        }

        AgentRuntime runtime = new AgentRuntime(
                apiKey,
                registry,
                DEFAULT_MAX_LOOPS,
                null
        );
        MessageStore messageStore = new MessageStore(maxTurns);
        return new RuntimeBundle(runtime, messageStore);
    }

    public record RuntimeBundle(AgentRuntime runtime, MessageStore messageStore) {}
}
