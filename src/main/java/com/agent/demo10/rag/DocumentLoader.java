package com.agent.demo10.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

public class DocumentLoader {

    private final Path knowledgeBaseDir;
    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentLoader(Path knowledgeBaseDir, int chunkSize, int chunkOverlap) {
        this.knowledgeBaseDir = knowledgeBaseDir;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 加载指定目录下的 md 文档，然后切分成 chunks（是 string 类型的 chunks 比较粗糙）
     */
    public List<DocumentChunk> loadKnowledgeBase() throws IOException {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (!Files.exists(knowledgeBaseDir) || !Files.isDirectory(knowledgeBaseDir)) {
            throw new IllegalStateException("知识库目录不存在：" + knowledgeBaseDir);
        }

        try (Stream<Path> stream = Files.list(knowledgeBaseDir)) {// 读取 knowledgeBaseDir 这个目录下的内容
            // 找到 markdown 格式的文件
            List<Path> markdownFiles = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();

            for (Path path : markdownFiles) {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                List<String> parts = splitText(text);
                for (int i = 0; i < parts.size(); i++) {
                    String content = parts.get(i);
                    String source = path.getFileName().toString();
                    chunks.add(new DocumentChunk(
                            md5Hex(source + ":" + i + ":" + content),
                            source,
                            i,
                            content
                    ));
                }
            }
        }

        return chunks;
    }

    /**
     * 把文档切成 chunk 喂给向量模型
     */
    public List<String> splitText(String text) {
        String cleaned = text == null ? "" : text.strip();// 去掉首尾空白字符
        if (cleaned.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < cleaned.length()) {
            int end = Math.min(start + chunkSize, cleaned.length());
            String chunk = cleaned.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            if (end == cleaned.length()) {
                break;
            }
            start = Math.max(0, end - chunkOverlap);
        }
        return chunks;
    }

    private static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 MD5", ex);
        }
    }
}
