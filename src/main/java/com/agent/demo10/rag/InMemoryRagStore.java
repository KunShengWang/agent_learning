package com.agent.demo10.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryRagStore {

    private final DocumentLoader documentLoader;
    private final EmbeddingModel embeddingModel;
    private final List<IndexedChunk> index = new ArrayList<>();

    public InMemoryRagStore(DocumentLoader documentLoader, EmbeddingModel embeddingModel) {
        this.documentLoader = documentLoader;
        this.embeddingModel = embeddingModel;
    }

    public IndexStats rebuildIndex() throws IOException {
        index.clear();
        // 加载指定目录下的 md 文档，然后切分成 chunks（是 string 类型的 chunks 比较粗糙）
        List<DocumentChunk> chunks = documentLoader.loadKnowledgeBase();
        if (chunks.isEmpty()) {
            throw new IllegalStateException("knowledge_base 目录下没有可索引的 Markdown 文档。");
        }

        // 把 string 类型的 chunk 向量话然后存储起来
        for (DocumentChunk chunk : chunks) {
            index.add(new IndexedChunk(chunk, embeddingModel.embed(chunk.content())));
        }

        Set<String> sources = new HashSet<>();
        for (DocumentChunk chunk : chunks) {
            sources.add(chunk.source());
        }
        // 返回文档数和切块数
        return new IndexStats(sources.size(), chunks.size());
    }

    public List<RetrievedChunk> retrieve(String query, int topK) {
        // 把问题向量化
        double[] queryEmbedding = embeddingModel.embed(query);

        return index.stream()
                .map(item -> {
                    // 计算余弦相似度
                    double similarity = cosineSimilarity(queryEmbedding, item.embedding());
                    double distance = 1.0 - similarity;
                    DocumentChunk chunk = item.chunk();
                    return new RetrievedChunk(chunk.source(), chunk.chunkIndex(), chunk.content(), distance);
                })
                .sorted(Comparator.comparingDouble(RetrievedChunk::distance))
                .limit(topK)
                .toList();
    }

    /**
     * 计算余弦相似度
     */
    private static double cosineSimilarity(double[] left, double[] right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record IndexedChunk(DocumentChunk chunk, double[] embedding) {
    }

    public record IndexStats(int documents, int chunks) {
    }
}
