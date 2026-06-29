package com.agent.demo10;

import com.agent.demo10.llm.LlmClient;
import com.agent.demo10.rag.DocumentLoader;
import com.agent.demo10.rag.InMemoryRagStore;
import com.agent.demo10.rag.LocalHashEmbeddingModel;
import com.agent.demo10.rag.RetrievedChunk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class RagDemo {

    private static final Path KNOWLEDGE_BASE_DIR =
            Path.of("D:/JDK/IDEA/java_reinforcement_learning/agent_learning/src/main/java/com/agent/demo10/knowledge_base")
            .toAbsolutePath()
            .normalize();
    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 120;
    private static final int EMBEDDING_DIMENSION = 256;
    private static final int TOP_K = 4;

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行："
                            + " $env:DEEPSEEK_API_KEY=\"你的 API Key\""
            );
        }

        System.out.println("RAG Demo 已启动。正在加载本地知识库并构建内存索引...");
        InMemoryRagStore ragStore = new InMemoryRagStore(
                new DocumentLoader(KNOWLEDGE_BASE_DIR, CHUNK_SIZE, CHUNK_OVERLAP),
                new LocalHashEmbeddingModel(EMBEDDING_DIMENSION)
        );

        // 加载指定目录下的文档并进行切块，然后对切块进行向量化然后存储，最后返回文档数和切块数
        InMemoryRagStore.IndexStats stats = ragStore.rebuildIndex();
        System.out.println("索引构建完成：documents=" + stats.documents() + "，chunks=" + stats.chunks());
        System.out.println("输入 exit 或 quit 结束。");
        System.out.println("你可以试试：ReAct Agent 和普通聊天机器人有什么区别？");

        LlmClient llmClient = new LlmClient(apiKey);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("\n你：");
            String question = reader.readLine();
            if (question == null) {
                break;
            }

            question = question.trim();
            if ("exit".equalsIgnoreCase(question) || "quit".equalsIgnoreCase(question)) {
                System.out.println("已结束。");
                break;
            }
            if (question.isBlank()) {
                continue;
            }

            try {
                String answer = answerWithRag(llmClient, ragStore, question);
                System.out.println("\n助手：" + answer);
            } catch (Exception ex) {
                System.out.println("回答失败：" + ex.getMessage());
            }
        }
    }

    private static String answerWithRag(LlmClient llmClient, InMemoryRagStore ragStore, String question)
            throws Exception {
        // 召回向量数据库与问题相似的前 k 个 chunks
        List<RetrievedChunk> chunks = ragStore.retrieve(question, TOP_K);
        String contextText = buildContextText(chunks);

        String systemPrompt = ""
                + "你是一个 RAG 教程助手。"
                + "回答问题时必须优先依据提供的资料。"
                + "如果资料里没有答案，要明确说资料不足，不要编造。"
                + "回答使用简洁清晰的中文。";
        String userContent = "用户问题：" + question + "\n\n可参考资料：\n" + contextText;

        return llmClient.askText(systemPrompt, userContent);
    }

    private static String buildContextText(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "没有检索到相关资料。";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            if (i > 0) {
                builder.append("\n\n");
            }
            builder.append("[资料 ").append(i + 1).append("]\n");
            builder.append("来源：")
                    .append(chunk.source())
                    .append("，chunk_index=")
                    .append(chunk.chunkIndex())
                    .append("，distance=")
                    .append(String.format("%.4f", chunk.distance()))
                    .append("\n");
            builder.append(chunk.content());
        }
        return builder.toString();
    }
}
