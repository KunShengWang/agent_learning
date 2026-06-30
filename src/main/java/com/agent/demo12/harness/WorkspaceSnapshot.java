package com.agent.demo12.harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public record WorkspaceSnapshot(
        Map<String, String> fileHashes
) {
    /**
     * WorkspaceSnapshot 持有的就是一个 Map<String, String>，长这样：
     * {
     *   "pom.xml"                            → "a3f5b2...",
     *   "src/Main.java"                      → "e8c1d9...",
     *   "src/GreetingService.java"           → "7b4f02...",
     *   ...
     * }
     */
    public static WorkspaceSnapshot capture(Path workspaceDir) throws IOException {
        Map<String, String> hashes = new HashMap<>();
        // 递归遍历工作区目录下所有文件（包括子目录）
        try (Stream<Path> stream = Files.walk(workspaceDir)) {
            // 只留普通文件，跳过目录本身
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String relativePath = workspaceDir.relativize(path).toString().replace("\\", "/");
                hashes.put(relativePath, sha256(path));// 把文件内容算成一个固定 64 字符的哈希字符串，作为文件的"指纹"
            }
        }
        return new WorkspaceSnapshot(Map.copyOf(hashes));
    }

    public Set<String> changedFilesComparedTo(WorkspaceSnapshot other) {
        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(fileHashes.keySet());
        allPaths.addAll(other.fileHashes.keySet());

        Set<String> changed = new TreeSet<>();
        for (String path : allPaths) {
            String currentHash = fileHashes.get(path);
            String otherHash = other.fileHashes.get(path);
            if (!String.valueOf(currentHash).equals(String.valueOf(otherHash))) {
                changed.add(path);
            }
        }
        return changed;
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }
}

