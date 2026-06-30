package com.agent.demo13.context;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public record RunContext(
        String userId,
        String displayName,// 用户称呼
        LocalDate today,
        Set<String> permissions,// 允许能力
        Map<String, String> secrets,// 各种密钥，不能发送给 LLM
        Path demoRoot
) {
    public String safeDebugView() {
        return ""
                + "userId=" + userId + "\n"
                + "displayName=" + displayName + "\n"
                + "today=" + today + "\n"
                + "permissions=" + permissions + "\n"
                + "demoRoot=" + demoRoot + "\n"
                + "secrets=" + secrets.keySet() + "  <-- 只显示 key，不显示 value";
    }

    /**
     * 安全投影后的本地上下文，去除敏感信息
     */
    public String safeProjectionForPrompt() {
        return ""
                + "用户称呼: " + displayName + "\n"
                + "当前日期: " + today + "\n"
                + "允许能力: " + permissions + "\n"
                + "注意: 本地 secrets、API key、数据库连接串不能放进 prompt。";
    }
}
