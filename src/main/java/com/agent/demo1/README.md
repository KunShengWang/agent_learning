# Java Demo1：最小 LLM 调用

这个目录是 `demo1/hello_world.py` 的 Java 版本。目标不是引入 Agent 框架，而是用 Java 把一次最小的大模型调用跑通。

## 这一节要学什么

一次最小 LLM 调用只需要四件事：

1. 准备 `system` 消息，告诉模型身份和行为规则。
2. 准备 `user` 消息，表达用户问题。
3. 把 `model`、`messages`、`temperature`、`max_tokens` 等参数组装成 JSON 请求。
4. 发送 HTTP 请求，并读取 `choices[0].message.content`。

## Python 到 Java 的对应关系

| Python | Java |
| --- | --- |
| `dict[str, str]` | `record ChatMessage(String role, String content)` |
| `list[dict[str, str]]` | `List<ChatMessage>` |
| `requests.post(...)` | `HttpClient.send(...)` |
| `json.dumps(...)` | `ObjectMapper.writeValueAsString(...)` |
| `response.json()` | `ObjectMapper.readTree(...)` |
| `os.getenv(...)` | `System.getenv(...)` |

## 运行方式

在 PowerShell 中配置 API Key：

```powershell
$env:DEEPSEEK_API_KEY="你的 API Key"
```

然后运行：

```powershell
cd java-demo1
mvn compile
java -cp target/classes com.example.agenttutorial.demo1.DeepSeekHelloWorld
```

如果只想先检查能否编译：

```powershell
cd java-demo1
mvn compile
```

这个版本只使用 JDK 17 标准库，不依赖 Jackson、Spring AI、LangChain4j 等库。为了保持 demo1 足够底层，代码里手写了最小 JSON 请求构造和响应字段读取。真实项目里可以换成 Jackson。

## 和 Python 版的核心一致性

Java 版保留了 Python 版的核心闭环：

```text
buildMessages()
 -> callLlm()
 -> 解析 choices[0].message.content
 -> 打印 token usage
```

这里还不是 Agent，只是 Agent 的地基：一次稳定、可理解、可调试的模型调用。
