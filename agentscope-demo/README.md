# AgentScope Demo — 三国狼人杀游戏

基于 **Java 21 + Spring Boot 3.3 + Spring AI 1.0** 实现 [AgentScope](https://github.com/modelscope/agentscope) 框架的核心设计理念，演示消息驱动架构在多智能体游戏场景中的应用。

> AgentScope 是阿里巴巴达摩院开发的纯 Python 多智能体框架，无 Java SDK。本项目基于其核心理念用 Java 重新实现。

## 案例场景

融合中国古典文化元素的"三国狼人杀"游戏，6 名三国人物扮演狼人杀角色：

| 三国人物 | 游戏角色 | 阵营 | 能力 |
|----------|----------|------|------|
| 孙权 | 狼人 | 狼人 | 夜晚协商击杀目标 |
| 周瑜 | 狼人 | 狼人 | 夜晚协商击杀目标 |
| 曹操 | 预言家 | 好人 | 夜晚查验一名玩家身份 |
| 张飞 | 女巫 | 好人 | 拥有解药和毒药各一瓶 |
| 司马懿 | 村民 | 好人 | 通过推理识别狼人 |
| 赵云 | 村民 | 好人 | 通过推理识别狼人 |

每个智能体具有**双重身份**：游戏功能角色 + 三国人格——"曹操"扮演的预言家会比"赵云"更狡猾地公开查验结果。

## AgentScope 核心理念的 Java 实现

### 1. 消息驱动架构

所有交互抽象为 `Msg` 的发送和接收，而非传统的函数调用：

```java
// AgentScope 的 Msg → Java record
public record Msg(String name, String content, String role,
                  Map<String, String> metadata, Instant timestamp) {}
```

### 2. MsgHub 消息中心

对应 AgentScope 的核心创新——通过 MsgHub 实现灵活的消息路由：

```java
// 建立狼人专属私密通信频道
try (MsgHub wolfHub = MsgHub.create(wolfAgents, announcement)) {
    wolfHub.setMessageCallback(msg -> printLine(msg.name() + ": " + msg.content()));
    Pipeline.sequential(wolfHub, wolfAgents, discussionPrompt);  // 讨论
    wolfHub.setAutobroadcast(false);                              // 关闭广播
    // ... 独立投票（不互相影响）
}

// 白天全体公开讨论
try (MsgHub dayHub = MsgHub.create(allAgents, dayAnnouncement)) {
    Pipeline.sequential(dayHub, allAgents, discussionPrompt);
}
```

### 3. Agent 生命周期

对应 AgentScope 的 `AgentBase`，两个核心方法：

- `reply(Msg)` — 智能体收到消息后"思考并回应"
- `observe(Msg)` — 被动接收消息，加入记忆但不回复

### 4. Pipeline 工作流

- `Pipeline.sequential()` — 顺序管线，智能体依次发言（白天讨论）
- `Pipeline.fanout()` — 扇出管线，并行收集所有智能体回复（投票阶段）

### 5. 结构化输出

通过 JSON 格式提示约束 LLM 输出，模拟 AgentScope 的 Pydantic 结构化输出：

```java
// 投票决策
{"target": "曹操"}

// 女巫行动
{"use_antidote": true, "use_poison": false, "poison_target": null}

// 讨论分析
{"analysis": "...", "suspect": "孙权", "confidence": 8}
```

## 游戏流程

```
┌──────── 初始化 ────────┐
│ 分配角色 + 通知狼人队友 │
└──────────┬─────────────┘
           ▼
┌──────── 夜晚 ────────┐     ┌──── 狼人阶段 ────┐
│                       │────▶│ MsgHub 私密频道   │
│  狼人→预言家→女巫     │     │ 讨论 + 投票击杀   │
│                       │     └──────────────────┘
│                       │────▶ 预言家查验（点对点）
│                       │────▶ 女巫用药（结构化输出）
└──────────┬────────────┘
           ▼
     结算夜晚结果
           ▼
┌──────── 白天 ────────┐     ┌──── 讨论阶段 ────┐
│                       │────▶│ MsgHub 全体广播   │
│  讨论→投票→淘汰       │     │ sequential_pipeline│
│                       │     └──────────────────┘
│                       │────▶ fanout_pipeline 并行投票
│                       │────▶ 统计票数，淘汰最高票
└──────────┬────────────┘
           ▼
     胜负判定 ──→ 未结束则回到夜晚
```

## 项目结构

```
com.example.agentscope/
├── AgentScopeDemoApplication.java     # Spring Boot 启动类
├── agent/                             # 智能体（AgentScope 核心抽象）
│   ├── Agent.java                     #   接口：reply() + observe()
│   └── DialogAgent.java              #   LLM 对话智能体（内置记忆）
├── model/
│   └── SpringAIChatClient.java        #   封装 Spring AI ChatClient
├── message/
│   └── Msg.java                       #   消息 record（name, content, role, metadata）
├── chat/                              # 消息驱动协调
│   ├── MsgHub.java                    #   消息中心（广播/动态参与者/消息历史）
│   └── Pipeline.java                  #   顺序管线 + 扇出管线
├── game/                              # 游戏逻辑
│   ├── GameRole.java                  #   角色枚举（狼人/预言家/女巫/村民）
│   ├── Player.java                    #   玩家 record（人物×角色×智能体×存活）
│   ├── RolePrompts.java               #   提示词工厂 + 结构化输出格式
│   └── ThreeKingdomsWerewolfGame.java #   游戏控制器（三层架构）
└── runner/
    └── WerewolfGameRunner.java        #   CommandLineRunner 入口
```

## 快速开始

### 1. 环境准备

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export LLM_MODEL_ID=qwen-turbo
```

### 2. 编译运行

```bash
# 在项目根目录
mvn clean compile -pl agentscope-demo
mvn spring-boot:run -pl agentscope-demo
```

### 3. 预期输出

```
🎮 欢迎来到三国狼人杀！

=== 游戏初始化 ===
游戏主持人: 📢 【孙权】你在这场三国狼人杀中扮演狼人...
游戏主持人: 📢 【周瑜】你在这场三国狼人杀中扮演狼人...
...

=== 第1轮游戏 ===
🌙 第1夜降临，天黑请闭眼...

【狼人阶段】
孙权: 今晚我们应该除掉曹操，此人智谋过人...
周瑜: 孙权所言极是，曹操势力庞大...

【预言家阶段】
曹操: 我要查验孙权。
游戏主持人: 📢 查验结果：孙权是狼人

【女巫阶段】
张飞: 我使用解药救人！

【白天讨论阶段】
曹操: 我昨晚查验了孙权，他是狼人！
孙权: 曹操在说谎，我怎么可能是狼人...

【投票阶段】
...

🎉 好人阵营获胜！/ 🐺 狼人阵营获胜！
```

## 对应 AgentScope Python 概念映射

| AgentScope (Python) | 本项目 (Java) | 说明 |
|---------------------|---------------|------|
| `Msg` | `Msg` (record) | 消息驱动的基本单元 |
| `AgentBase` | `Agent` (interface) | `reply()` + `observe()` |
| `DialogAgent` | `DialogAgent` | LLM 对话智能体，内置记忆 |
| `MsgHub` | `MsgHub` (AutoCloseable) | try-with-resources 管理生命周期 |
| `sequential_pipeline` | `Pipeline.sequential()` | 顺序执行 |
| `fanout_pipeline` | `Pipeline.fanout()` | 并行收集回复 |
| `agentscope.init()` | Spring Boot 自动配置 | 模型初始化 |
| Pydantic `BaseModel` | JSON 格式提示 + 正则解析 | 结构化输出约束 |

## 依赖

| 依赖 | 用途 |
|------|------|
| spring-boot-starter-web | Spring Boot 基础 |
| spring-ai-starter-model-openai | LLM 调用（OpenAI 兼容接口） |
