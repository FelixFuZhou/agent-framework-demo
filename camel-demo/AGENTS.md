# AGENTS.md — CAMEL Demo 模块上下文（L4 模块级）

## 模块简介

基于 CAMEL（Communicative Agents for "Mind" Exploration of Large Language Model Society）论文的核心理念，使用 Java 21 + Spring Boot 3.3 + Spring AI 1.0 实现角色扮演（Role-Playing）协作机制。

案例场景：两个 AI 智能体（心理学家 + 科普作家）通过 Inception Prompting 自主协作，生成一本关于"拖延症心理学"的科普电子书。

## CAMEL 框架核心理念

CAMEL 是由 KAUST（阿卜杜拉国王科技大学）提出的多智能体协作框架，核心创新是 **Inception Prompting（启始提示）**：

1. **角色扮演范式**：将任务分配给 AI User（指挥者）和 AI Assistant（执行者），两者通过对话自主完成任务
2. **Inception Prompt**：精心设计的系统提示词让 AI 自发进入角色，无需人工干预
3. **轻架构、重提示**：框架代码极简，核心价值在提示词工程
4. **完全自主**：AI User 替代人类用户，实现端到端无人值守的任务执行

## 与 Python CAMEL 的对应关系

| Python CAMEL | Java 实现 | 说明 |
|---|---|---|
| `ChatAgent` | `ChatAgent` 接口 + `CamelAgent` 实现 | 基于 step() 的单步对话智能体 |
| `RolePlaying` | `RolePlaying` 类 | 角色扮演协调器，管理双智能体协作 |
| Inception Prompt | `InceptionPrompts` 工具类 | AI User / AI Assistant / Task Specifier 三套提示模板 |
| `ModelBackend` | `SpringAIChatClient` | 模型调用封装（Spring AI 替代 OpenAI SDK） |
| `ChatMessage` | `ChatMessage` record | 消息载体，使用 role 标识角色 |
| `<CAMEL_TASK_DONE>` | `RolePlaying.TASK_DONE` 常量 | 终止信号 |
| `task_type=TaskType.AI_SOCIETY` | Inception Prompt 模板 | 通过模板区分任务类型 |

## 目录结构

```
camel-demo/src/main/java/com/example/camel/
├── CamelDemoApplication.java       # Spring Boot 启动类
├── agent/
│   ├── ChatAgent.java              # 智能体接口（step 单步对话）
│   └── CamelAgent.java             # 智能体实现（独立记忆 + Inception Prompt）
├── chat/
│   ├── InceptionPrompts.java       # Inception Prompting 提示词模板
│   └── RolePlaying.java            # 角色扮演协调器（核心机制）
├── config/
│   └── CamelConfig.java            # Spring 配置（ChatClient Bean）
├── message/
│   └── ChatMessage.java            # 消息记录（role + content）
├── model/
│   └── SpringAIChatClient.java     # 模型客户端（封装 Spring AI）
└── runner/
    └── EbookRunner.java            # 电子书生成运行器（CommandLineRunner）
```

## 核心机制：角色扮演协作流程

```
┌────────────────────────────────────────────────────┐
│                  RolePlaying 协调器                  │
├────────────────────────────────────────────────────┤
│                                                    │
│  1. Task Specifier: 细化任务描述                     │
│          ↓                                         │
│  2. 构建 Inception Prompts (AI User + AI Assistant) │
│          ↓                                         │
│  3. initChat(): AI User 发出第一条指示               │
│          ↓                                         │
│  ┌──────────────── 协作循环 ────────────────┐      │
│  │                                          │      │
│  │  AI User ──指示──→ AI Assistant           │      │
│  │     ↑                    │               │      │
│  │     └────解决方案────────┘               │      │
│  │                                          │      │
│  │  终止条件: <CAMEL_TASK_DONE> 或达到最大轮次 │      │
│  └──────────────────────────────────────────┘      │
│                                                    │
└────────────────────────────────────────────────────┘
```

## 关键设计决策

1. **独立记忆 vs 共享历史**：每个 CamelAgent 维护独立的 memory 列表，对方消息标记为 "user"，自己回复标记为 "assistant"，实现视角翻转
2. **Task Specifier**：可选的任务细化步骤，将粗粒度任务描述转化为具体可执行的方案
3. **纯 CLI 模式**：设置 `spring.main.web-application-type=none`，通过 CommandLineRunner 直接运行
4. **record 消息**：使用 Java record 实现不可变消息对象

## 运行方式

```bash
cd camel-demo
mvn spring-boot:run
```

需要设置环境变量：
```bash
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_MODEL_ID=qwen-plus
```
