# Context Engineering Notes

Context Engineering 关注的是：在每次调用模型之前，系统如何选择、压缩、排序和呈现上下文。

它不是简单地把所有历史消息、所有工具结果、所有记忆都塞进 prompt。上下文窗口是有限的，错误或过多的信息会让模型分心，甚至泄露不该给模型看的本地状态。

一个 Agent 系统通常同时拥有两种上下文：

1. Local Run Context
   这部分在程序本地使用，例如用户 ID、权限、API Key、运行目录、临时缓存、数据库连接、预算限制。
   这些信息不一定都应该发送给模型。

2. LLM Prompt Context
   这部分是真正放进 messages 的内容，例如 system 指令、用户目标、相关记忆、近期对话、必要工具结果、可用工具说明。

Context Engineering 的常见动作包括：

- 只选择和当前任务相关的长期记忆。
- 把旧对话压缩成摘要，只保留最近几轮原文。
- 对很长的工具结果做裁剪，只保留关键片段。
- 不把密钥、内部路径、数据库连接串等敏感本地状态发送给模型。
- 按任务需要选择工具，不把所有工具都暴露给模型。
- 在上下文预算不足时，优先保留高价值信息。

和 RAG 的关系：

RAG 是 Context Engineering 的一种常见手段。RAG 负责从外部知识库检索相关资料，然后把检索结果作为上下文的一部分放进 prompt。

和 Memory 的关系：

Memory 只是上下文来源之一。真正关键的是：什么时候取 memory，取哪几条，怎么放进 prompt，以及什么时候不放。

和 Harness 的关系：

Harness 可以评估 Agent 的行为；Context Engineering 可以优化 Agent 每次调用模型时看到的信息。两者结合后，可以比较不同上下文策略对 Agent 成功率的影响。
