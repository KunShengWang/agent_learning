# Java Agent 学习计划

## 一、前置知识
- [ ] Java 基础（反射、字节码、类加载机制）
- [ ] JVM 基础知识（内存模型、GC、类加载器）
- [ ] 熟悉 Maven/Gradle 构建工具

## 二、第一阶段：入门基础（第1周）
- [ ] 了解 Java Agent 的概念与用途
- [ ] 学习 premain 与 agentmain 两种启动方式
- [ ] 编写第一个简单的 Java Agent（打印类加载信息）
- [ ] 掌握 MANIFEST.MF 配置（Premain-Class、Agent-Class、Can-Retransform-Classes）

## 三、第二阶段：字节码增强（第2-3周）
- [ ] 学习 Instrumentation API（addTransformer、retransformClasses）
- [ ] 掌握 ASM 字节码框架基础
- [ ] 掌握 Javassist 字节码操作库
- [ ] 实现方法耗时统计 Agent
- [ ] 实现方法入参/返回值打印 Agent

## 四、第三阶段：高级应用（第4周）
- [ ] 学习 ByteBuddy 框架（更简洁的字节码操作）
- [ ] 实现热替换 Agent（动态修改已加载类）
- [ ] 实现 AOP 风格的拦截 Agent
- [ ] 了解 Java Agent 在 APM（如 SkyWalking、Pinpoint）中的应用

## 五、第四阶段：实战项目（第5-6周）
- [ ] 实现一个简易的链路追踪 Agent
- [ ] 实现一个 SQL 监控 Agent
- [ ] 实现一个方法调用统计与可视化 Agent
- [ ] 编写单元测试与集成测试

## 六、参考资料
- 《深入理解 Java 虚拟机》（周志明）
- Java Instrumentation API 官方文档
- ASM / Javassist / ByteBuddy 官方文档
- 开源 APM 项目源码（SkyWalking、Arthas）

## 七、学习建议
- 每学完一个知识点，立即动手写 Demo 验证
- 遇到问题优先查阅官方文档和源码
- 多阅读开源项目中的 Agent 实现代码
