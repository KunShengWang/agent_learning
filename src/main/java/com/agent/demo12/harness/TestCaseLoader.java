package com.agent.demo12.harness;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class TestCaseLoader {

    /**
     * 加载所有测试用例，也就是所有 .properties 文件
     */
    public List<TestCase> loadAll(Path testCasesDir) throws IOException {
        try (Stream<Path> stream = Files.list(testCasesDir)) {
            // 找到以 .properties 为后缀的文件路径
            List<Path> paths = stream
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            List<TestCase> testCases = new ArrayList<>();
            for (Path path : paths) {
                // 把以 .properties 为后缀的文件加载成 TestCase 对象
                testCases.add(loadOne(path));
            }
            return List.copyOf(testCases);
        }
    }

    /**
     * 加载单个测试用例
     */
    private TestCase loadOne(Path path) throws IOException {
        // 把 .properties 配置文件读进内存，解析成键值对结构
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        List<TestCase.ExpectedContent> expectedContents = new ArrayList<>();
        int index = 1;
        while (properties.containsKey("expected.contains." + index + ".path")) {
            expectedContents.add(new TestCase.ExpectedContent(
                    required(properties, "expected.contains." + index + ".path"),
                    required(properties, "expected.contains." + index + ".text")
            ));
            index++;
        }

        return new TestCase(
                required(properties, "id"),
                properties.getProperty("fixture_dir", "basic-java-app").trim(),
                required(properties, "goal"),
                csv(properties.getProperty("expected.tool_sequence", "")),
                csv(properties.getProperty("allowed.changed_files", "")),
                csv(properties.getProperty("expected.final_contains", "")),
                Boolean.parseBoolean(properties.getProperty("expected.compile", "false")),
                List.copyOf(expectedContents)
        );
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少测试用例字段: " + key);
        }
        return value.trim();
    }

    private List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return List.copyOf(items);
    }
}

