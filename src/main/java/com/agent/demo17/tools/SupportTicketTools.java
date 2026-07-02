package com.agent.demo17.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SupportTicketTools {

    private final Map<String, String> tickets = new LinkedHashMap<>();

    public SupportTicketTools() {
        tickets.put("T-1001", "工单 T-1001：支付服务登录失败，状态=处理中，负责人=张三，下一步=检查认证服务日志。");
        tickets.put("T-1002", "工单 T-1002：订单导出超时，状态=待排查，负责人=李四，下一步=确认数据库慢查询。");
    }

    @Tool(description = "根据工单编号查询工单状态、负责人和下一步处理建议。只有用户要查询已有工单时才调用。")
    public String queryTicketStatus(@ToolParam(description = "工单编号，例如 T-1001") String ticketId) {
        System.out.println("[Tool Executed] queryTicketStatus(ticketId=" + ticketId + ")");
        return tickets.getOrDefault(ticketId, "没有找到工单：" + ticketId);
    }

    @Tool(description = "创建一个新的故障工单草稿，返回新工单编号。只有用户明确要求创建工单时才调用。")
    public String createTicketDraft(@ToolParam(description = "工单标题") String title,
                                    @ToolParam(description = "优先级，例如 高、中、低") String priority) {
        System.out.println("[Tool Executed] createTicketDraft(title=" + title + ", priority=" + priority + ")");

        String ticketId = "T-" + DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now());
        String ticket = "新建工单 " + ticketId + "：标题=" + title + "，优先级=" + priority + "，状态=草稿";
        tickets.put(ticketId, ticket);
        return ticket;
    }
}
