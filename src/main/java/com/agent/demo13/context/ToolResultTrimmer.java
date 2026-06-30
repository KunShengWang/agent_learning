package com.agent.demo13.context;

public class ToolResultTrimmer {

    public String trim(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        int head = Math.max(0, maxChars / 2);
        int tail = Math.max(0, maxChars - head);
        return content.substring(0, head)
                + "\n...[工具结果过长，已裁剪]...\n"
                + content.substring(content.length() - tail);
    }
}
