# Java Demo10：RAG Agent

这个目录是 `demo10` 的 Java 学习版。它把 Agent 从“只依赖上下文窗口”推进到“回答前先检索知识库”。

## 这一节要学什么

RAG 的核心不是让模型凭记忆回答，而是：

```text
先检索资料 -> 再把资料放进 prompt -> 最后让 LLM 基于资料回答
```

完整链路是：

```text
加载文档 -> 切分 chunk -> 向量化 -> 建索引 -> 检索 Top-K -> 生成答案
```

## 和 Python demo10 的区别

Python 版本使用：

```text
智谱 embedding-3 + PostgreSQL pgvector
```

Java 学习版为了降低环境成本，使用：

```text
本地 Hash 向量 + 内存检索
```

两者思想相同：

```text
文档和问题必须进入同一个向量空间，然后按相似度取回相关资料。
```

后面如果要接真实工程，只需要把 `LocalHashEmbeddingModel` 换成真实 embedding API，把 `InMemoryRagStore` 换成 pgvector / Elasticsearch / Milvus 等存储即可。

## 关键文件

```text
src/main/java/com/example/agenttutorial/demo10/
  RagDemo.java
  llm/
    LlmClient.java
    JsonUtil.java
  rag/
    DocumentLoader.java
    DocumentChunk.java
    EmbeddingModel.java
    LocalHashEmbeddingModel.java
    InMemoryRagStore.java
    RetrievedChunk.java
```

## 运行方式

```powershell
cd D:\JDK\IDEA\java_reinforcement_learning\agent-tutorial\java-demo10
$env:DEEPSEEK_API_KEY="你的 API Key"
mvn compile
java -cp target/classes com.example.agenttutorial.demo10.RagDemo
```

可以这样测试：

```text
你：ReAct Agent 和普通聊天机器人有什么区别？
你：什么时候需要 HITL？
你：Workflow 和 ReAct 适合的场景有什么不同？
```

## 本节核心理解

```text
RAG Agent = 外部知识库检索 + 检索结果注入 prompt + 基于资料回答
```

它解决的不是“让模型更聪明”，而是让模型回答前先拿到可靠资料，从而减少凭空编造。
