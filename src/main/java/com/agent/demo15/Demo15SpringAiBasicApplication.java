package com.agent.demo15;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@SpringBootApplication(scanBasePackageClasses = Demo15SpringAiBasicApplication.class)
public class Demo15SpringAiBasicApplication implements ApplicationRunner {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public Demo15SpringAiBasicApplication(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public static void main(String[] args) {
        SpringApplication.run(Demo15SpringAiBasicApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("缺少环境变量 DEEPSEEK_API_KEY。");
            System.out.println("PowerShell 示例：$env:DEEPSEEK_API_KEY=\"你的 API Key\"");
            return;
        }

        List<Message> messages = List.of(
                new SystemMessage("你是一个面向初学者的 Java 和 agent 助手。回答要简洁。"),
                new UserMessage("请用一句话介绍什么是 Agent，并给一个生活中的类比。")
        );
        // System.out.println("=== 发送给模型的 Message 的 json 形式 ===");
        // String messagesJson = objectMapper.writeValueAsString(messages);
        // System.out.println("\n[调试] MessageJson = " + messagesJson);


        System.out.println("\n=== 发送给模型的 messages ===");
        for (Message message : messages) {
            System.out.println(message.getMessageType() + ": " + message.getText());
        }

        ChatResponse response = chatModel.call(new Prompt(messages));
         System.out.println("\n[调试] response = " + objectMapper.writeValueAsString(response));
        // System.out.println("\n[调试] response.getResult() = " + objectMapper.writeValueAsString(response.getResult()));
        // System.out.println("\n[调试] response.getResult().getOutput() = " + objectMapper.writeValueAsString(response.getResult().getOutput()));

        String answer = response.getResult().getOutput().getText();
        // System.out.println("\n[调试] answer = " + answer);

        System.out.println("\n=== 模型回复 ===");
        System.out.println(answer);

        printUsage(response);
    }

    private void printUsage(ChatResponse response) throws Exception{
        ChatResponseMetadata metadata = response.getMetadata();
        // System.out.println("\n[调试] metadata = " + objectMapper.writeValueAsString(metadata));
        if (metadata == null || metadata.getUsage() == null) {
            return;
        }

        Usage usage = metadata.getUsage();
        System.out.println("\n=== Token 用量 ===");
        System.out.println("promptTokens = " + usage.getPromptTokens());
        System.out.println("completionTokens = " + usage.getCompletionTokens());
        System.out.println("totalTokens = " + usage.getTotalTokens());
    }
}
