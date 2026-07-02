package com.agent.demo17;

import com.agent.demo17.tools.SupportTicketTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackageClasses = Demo17ToolCallingApplication.class)
public class Demo17ToolCallingApplication implements ApplicationRunner {

    private final ChatClient chatClient;
    private final SupportTicketTools supportTicketTools;

    public Demo17ToolCallingApplication(ChatClient.Builder chatClientBuilder,
                                        SupportTicketTools supportTicketTools) {
        this.chatClient = chatClientBuilder.build();
        this.supportTicketTools = supportTicketTools;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Demo17ToolCallingApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    @Override
    public void run(ApplicationArguments args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行：");
            System.out.println("$env:DEEPSEEK_API_KEY=\"你的 API Key\"");
            return;
        }

        runQuestion("请查询工单 T-1001 的状态，并告诉我下一步应该做什么。");
        runQuestion("请创建一个高优先级工单，标题是支付服务无法登录。");
    }

    private void runQuestion(String question) {
        System.out.println("\n==============================");
        System.out.println("[User] " + question);

        String answer = chatClient.prompt()
                .system("""
                        你是一个工单助手。
                        只要用户问题涉及查询工单、创建工单、工单状态、故障处理，就必须优先调用可用工具。
                        不要凭空编造工单状态或工单编号。
                        工具返回结果后，再用简洁中文给用户最终答复。
                        """)
                .user(question)
                .tools(supportTicketTools)
                .call()
                .content();

        System.out.println("[Assistant] " + answer);
    }
}
