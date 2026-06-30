package com.agent.demo13.context;

import java.util.List;
import java.util.Set;

public class ToolCatalog {

    public List<ToolSpec> selectTools(ContextPolicy policy, List<ContextDecision> decisions) {
        List<ToolSpec> allTools = List.of(
                new ToolSpec("read_docs", "读取 demo13/docs 下的学习资料", "low"),
                new ToolSpec("summarize_notes", "把长资料压缩成摘要", "low"),
                new ToolSpec("delete_file", "删除本地文件", "high")
        );
        // 查询能暴露给 LLM 的工具
        Set<String> exposed = policy.exposedTools();
        // 对现有工具进行过滤，返回安全的工具
        return allTools.stream()
                .filter(tool -> {
                    boolean keep = exposed.contains(tool.name());
                    decisions.add(new ContextDecision(
                            "tool:" + tool.name(),
                            keep ? "include" : "drop",
                            keep ? "当前任务需要暴露这个工具" : "当前任务不需要，或者风险过高"
                    ));
                    return keep;
                })
                .toList();
    }
}
