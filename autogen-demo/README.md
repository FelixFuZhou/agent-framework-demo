# AutoGen Demo — 软件开发团队多智能体协作

基于 **Java 21 + Spring Boot 3.3 + Spring AI 1.0** 实现 [AutoGen](https://github.com/microsoft/autogen) 框架的核心机制，演示多智能体轮询协作完成软件开发任务。

## 案例场景

模拟一个 4 人软件开发团队，通过 RoundRobinGroupChat（轮询群聊）协作：

| 角色 | 类型 | 职责 |
|------|------|------|
| ProductManager | AssistantAgent（LLM） | 需求分析、功能拆解、制定开发计划 |
| Engineer | AssistantAgent（LLM） | 编写完整可运行的代码 |
| CodeReviewer | AssistantAgent（LLM） | 代码审查、提出修改建议 |
| UserProxy | 用户代理（无 LLM） | 代表人类用户发起任务和最终验收 |

对话流程：`用户任务 → ProductManager → Engineer → CodeReviewer → UserProxy → ProductManager → ...`

当消息中出现 `TERMINATE` 关键词时，对话终止。

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                     Web Browser                         │
│  POST /api/chat/start → sessionId                       │
│  GET  /api/chat/stream/{id} → SSE EventSource           │
│  POST /api/chat/input → 用户回复                         │
│  POST /api/chat/generate → 提取代码生成工程               │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│              Spring Boot Application                     │
│                                                          │
│  Controller ──→ TeamChatService ──→ RoundRobinGroupChat  │
│       │              │                     │             │
│       │              ▼                     ▼             │
│       │     ChatSessionRepository    SoftwareDevTeamFactory
│       │         (Redis 持久化)         (创建 Agent)       │
│       │              │                     │             │
│       ▼              │                     ▼             │
│  SseConnectionManager│              SpringAIChatClient   │
│    (SSE + Pub/Sub)   │              (调用 LLM API)       │
└───────────┬──────────┴──────────────────────────────────┘
            │                    │
            ▼                    ▼
┌──────────────────┐   ┌──────────────────┐
│      Redis       │   │   LLM API        │
│  · 会话状态存储    │   │  (OpenAI 兼容)    │
│  · Pub/Sub 消息   │   │  DeepSeek/通义千问 │
│    广播 SSE 事件   │   │  Ollama/GPT...   │
└──────────────────┘   └──────────────────┘
```

### 事件驱动设计

核心思想：**把传统 while 循环拆成单步执行**，每步完成后状态写回 Redis。

1. `POST /start` 创建初始 `ChatSessionState`，存入 Redis，异步启动 `driveSession()`
2. `driveSession()` 循环调用 `RoundRobinGroupChat.executeStep()` 执行单步
3. 遇到 UserProxy → 状态设为 `WAITING_INPUT`，暂停等待用户输入
4. `POST /input` 提交用户回复 → 更新 Redis 状态 → 继续驱动
5. 每步产生的消息通过 Redis Pub/Sub 广播给所有节点的 SSE 连接

这种设计支持**多节点部署**：任何节点都可以处理任意请求，无需 sticky session。

## 项目结构

```
com.example.autogen/
├── AutoGenDemoApplication.java          # Spring Boot 启动类
│
├── agent/                               # 智能体定义
│   ├── Agent.java                       #   接口：getName() + reply(chatHistory)
│   ├── AssistantAgent.java              #   LLM 驱动的助理智能体（Spring AI ChatClient）
│   ├── UserProxyAgent.java              #   CLI 用户代理（交互/自动两种模式）
│   └── WebUserProxyAgent.java           #   Web 用户代理（BlockingQueue，遗留保留）
│
├── model/
│   └── SpringAIChatClient.java          #   封装 Spring AI ChatClient，统一 LLM 调用
│
├── message/
│   └── ChatMessage.java                 #   record(source, content, timestamp)
│
├── chat/                                # 群聊协调机制
│   ├── RoundRobinGroupChat.java         #   轮询群聊（同步 CLI + 单步事件驱动两种模式）
│   ├── TerminationCondition.java        #   终止条件函数式接口
│   ├── TextMentionTermination.java      #   关键词匹配终止
│   ├── ChatSessionState.java            #   可序列化会话状态（record，存 Redis）
│   ├── ChatSessionRepository.java       #   会话仓储接口
│   └── RedisChatSessionRepository.java  #   Redis 实现（JSON 序列化，TTL 2h）
│
├── config/                              # 基础设施配置
│   ├── RedisConfig.java                 #   Redis Pub/Sub 监听容器 + ObjectMapper
│   ├── SseConnectionManager.java        #   SSE 连接管理 + Redis 消息广播
│   └── SseMessageSubscriber.java        #   Redis Pub/Sub 订阅者 → 本地 SSE 推送
│
├── controller/
│   └── TeamChatController.java          #   REST API（start/stream/input/generate）
│
├── service/
│   ├── TeamChatService.java             #   事件驱动编排（startChat/submitInput/driveSession）
│   └── CodeExtractorService.java        #   从对话历史提取代码块并生成文件
│
├── runner/
│   └── SoftwareDevTeamRunner.java       #   CLI 模式入口（@ConditionalOnProperty cli）
│
└── team/
    └── SoftwareDevTeamFactory.java      #   团队工厂（创建 4 个角色 Agent）
```

## 核心组件说明

### Agent（智能体）

- `AssistantAgent`：将 System Message（角色定义）+ 对话历史发给 LLM，返回生成内容
- `UserProxyAgent`：不调用 LLM。CLI 模式下通过 Scanner 读取用户输入，自动模式下使用预设回复

### RoundRobinGroupChat（轮询群聊）

AutoGen 的核心协调机制。提供两种执行模式：

- **`run()`**：同步阻塞循环，适合 CLI 场景
- **`executeStep()`**：静态方法，执行单步后返回新状态，适合事件驱动 Web 场景

### ChatSessionState（会话状态）

Java record，包含恢复执行所需的全部上下文：

```java
record ChatSessionState(
    String sessionId,
    Status status,              // RUNNING / WAITING_INPUT / COMPLETED / ERROR
    List<ChatMessage> chatHistory,
    int turnCount,
    int participantIndex,       // 当前发言者索引
    int maxTurns,
    String terminationKeyword,
    List<String> participantNames,
    Instant createdAt,
    Instant updatedAt
)
```

### SSE 事件流

前端通过 `EventSource` 接收实时事件：

| 事件名 | 说明 |
|--------|------|
| `connected` | 连接已建立（握手确认） |
| `message` | 智能体发言，`{source, content, timestamp}` |
| `waiting_input` | 轮到用户输入 |
| `complete` | 协作结束，`{totalTurns}` |
| `error` | 异常信息 |

### 代码生成

`CodeExtractorService` 从对话历史中识别 Markdown 代码块，通过多种策略提取文件名：

1. 代码块内首行的 `// filename: path` 注释
2. 代码块前的 Markdown 文本（`` `file.java` ``、`**file.java**`、标题等）
3. 从 Java `package` + `class` 声明推断路径

## 快速开始

### 1. 环境准备

```bash
# JDK 21
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

# LLM API 配置（以通义千问为例）
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export LLM_MODEL_ID=qwen-turbo

# Redis（Docker 启动）
docker run -d --name redis-autogen -p 6379:6379 redis:latest redis-server --requirepass redis123
export REDIS_PASSWORD=redis123
```

### 2. 编译运行

```bash
# 在项目根目录
mvn clean compile -pl autogen-demo
mvn spring-boot:run -pl autogen-demo
```

访问 http://localhost:8080 打开 Web UI。

### 3. CLI 模式

```bash
export AUTOGEN_MODE=cli
mvn spring-boot:run -pl autogen-demo
```

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/start` | 启动协作，body: `{"task": "..."}` → `{"sessionId": "..."}` |
| GET | `/api/chat/stream/{sessionId}` | SSE 事件流 |
| POST | `/api/chat/input` | 提交回复，body: `{"sessionId": "...", "input": "..."}` |
| POST | `/api/chat/generate` | 生成代码，body: `{"sessionId": "...", "outputDir": "/tmp/xxx"}` |

## 依赖

| 依赖 | 用途 |
|------|------|
| spring-boot-starter-web | Web 服务 + SSE |
| spring-ai-starter-model-openai | LLM 调用（OpenAI 兼容接口） |
| spring-boot-starter-data-redis | 会话状态持久化 + Pub/Sub |
| jackson-datatype-jsr310 | `java.time.Instant` JSON 序列化 |

## 对应 AutoGen Python 概念映射

| AutoGen (Python) | 本项目 (Java) |
|-------------------|---------------|
| `AssistantAgent` | `AssistantAgent` |
| `UserProxyAgent` | `UserProxyAgent` |
| `OpenAIChatCompletionClient` | `SpringAIChatClient` |
| `RoundRobinGroupChat` | `RoundRobinGroupChat` |
| `TextMentionTermination` | `TextMentionTermination` |
| `ChatMessage` | `ChatMessage` (record) |
| `team.run(task)` | `teamChat.run(task, callback)` / `TeamChatService.startChat()` |
