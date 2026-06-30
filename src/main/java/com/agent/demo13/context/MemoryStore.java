package com.agent.demo13.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class MemoryStore {

    private final List<MemoryItem> memories;

    private MemoryStore(List<MemoryItem> memories) {
        this.memories = List.copyOf(memories);
    }

    public static MemoryStore load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<MemoryItem> items = new ArrayList<>();
        int index = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                items.add(new MemoryItem("memory-" + index, trimmed.substring(2)));
                index++;
            }
        }
        return new MemoryStore(items);
    }

    public List<MemoryItem> retrieveRelevant(String query, int topK) {
        Set<String> queryTokens = tokenize(query);
        return memories.stream()
                .sorted(Comparator.comparingInt((MemoryItem item) -> score(item, queryTokens)).reversed())
                .limit(topK)
                .toList();
    }

    private int score(MemoryItem item, Set<String> queryTokens) {
        Set<String> memoryTokens = tokenize(item.content());
        int score = 0;
        for (String token : queryTokens) {
            if (memoryTokens.contains(token)) {
                score++;
            }
        }
        if (item.content().contains("Java")) {
            score += 2;
        }
        if (item.content().contains("demo12") || item.content().contains("Harness")) {
            score += 2;
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+"))
                .stream()
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
