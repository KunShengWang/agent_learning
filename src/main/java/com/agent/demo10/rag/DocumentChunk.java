package com.agent.demo10.rag;

public record DocumentChunk(
        String chunkId,// 分块 ID，chunk id，全局唯一主键
        String source,// 来源，也就是文件名
        int chunkIndex,// 同一个文件切分后，这个 chunk 排第几（从 0 开始）
        String content// chunk 内容，粗糙的 string 类型的文本
) {
}
