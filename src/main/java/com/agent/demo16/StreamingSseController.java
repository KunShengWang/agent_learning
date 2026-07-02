package com.agent.demo16;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/demo16")
public class StreamingSseController {

    private final ChatModel chatModel;

    public StreamingSseController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 不调用 LLM 的假流式接口。
     * 先用它理解：Controller 返回 Flux<String> 后，客户端会一段一段收到数据。
     */
    @GetMapping(value = "/mock-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> mockStream() {
        List<String> chunks = List.of(
                "收到问题。\n",
                "正在分析上下文。\n",
                "正在选择执行路径。\n",
                "正在生成回答。\n",
                "完成。\n"
        );

        // 每 700ms 从 chunks 里取一个元素，按顺序输出，模拟流式响应
        return Flux.interval(Duration.ofMillis(700))// 创建一个“定时发射数字”的流，每 700 ms 发射一次，从 0 开始递增：0, 1, 2, 3...
                .take(chunks.size())// 限制发射次数，超过这个数量就自动结束流
                .map(index -> chunks.get(index.intValue()))// 把“数字序列”转换为真实数据，即 0 → chunks[0]；1 → chunks[1] 等
                // 本质：流开始执行的生命周期钩子
                .doOnSubscribe(subscription -> System.out.println("[demo16] mock-stream subscribed"))// 当有人“订阅这个 Flux”时触发，只执行一次
                // 本质：数据流经过时的“拦截器”
                .doOnNext(chunk -> System.out.println("[demo16] mock chunk: " + chunk.trim()))// 每发出一个元素就触发一次，不改变数据只做日志
                // 本质：流结束回调（生命周期终点）
                .doOnComplete(() -> System.out.println("[demo16] mock-stream completed"));// 当 Flux 正常结束时触发（take 结束）
    }

    /**
     * 调用 LLM 的真实流式接口。
     * ChatModel.stream(...) 返回 Flux<ChatResponse>，每个 ChatResponse 里通常只有一小段增量文本。
     */
    @PostMapping(value = "/llm-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> llmStream(@RequestBody(required = false) StreamRequest request) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return Flux.just("缺少环境变量 DEEPSEEK_API_KEY，请先在 PowerShell 中执行：$env:DEEPSEEK_API_KEY=\"你的 API Key\"\n");
        }

        String question = request == null || request.question() == null || request.question().isBlank()
                ? "请用三句话解释 Spring AI 的流式输出是什么。"
                : request.question().trim();

        List<Message> messages = List.of(
                new SystemMessage("你是一个面向初学者的 Java 和 Agent 框架助手。回答要简洁，并适合流式输出。"),
                new UserMessage(question)
        );

        Prompt prompt = new Prompt(messages);

        Flux<String> modelChunks = chatModel.stream(prompt)
                .map(this::extractText)
                .filter(text -> !text.isBlank())
                .doOnSubscribe(subscription -> {
                    System.out.println("[demo16] llm-stream subscribed");
                    System.out.println("[demo16] question: " + question);
                })
                .doOnNext(chunk -> System.out.println("[demo16] llm chunk: " + chunk))
                .doOnComplete(() -> System.out.println("[demo16] llm-stream completed"))
                .onErrorResume(error -> Flux.just("\n[ERROR] LLM 流式调用失败：" + error.getMessage() + "\n"));

        return Flux.concat(
                Flux.just("正在调用 LLM 流式接口...\n\n"),
                modelChunks,
                Flux.just("\n\n[done]\n")
        );
    }

    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }
}
