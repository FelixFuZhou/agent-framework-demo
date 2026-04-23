# agentscope-demo 模块上下文（L4 模块层）

## 模块概述

基于 AgentScope 框架设计理念的 Java 实现。案例：**三国狼人杀游戏**，6 个智能体通过 MsgHub 消息驱动协作，展示消息驱动架构在多智能体游戏场景中的应用。

AgentScope 是纯 Python 框架（无 Java SDK），本模块基于其核心理念使用 Spring AI `ChatClient` 实现。

## 文件结构与职责

```
com.example.agentscope/
├── AgentScopeDemoApplication.java          # Spring Boot 启动类
│
├── agent/                                  # 智能体定义（AgentScope 核心抽象）
│   ├── Agent.java                          #   接口：reply() + observe()（对应 AgentBase）
│   └── DialogAgent.java                    #   LLM 对话智能体（对应 DialogAgent），内置记忆管理
│
├── model/                                  # LLM 模型客户端
│   └── SpringAIChatClient.java             #   封装 Spring AI ChatClient
│
├── message/                                # 消息定义
│   └── Msg.java                            #   record: name + content + role + metadata（对应 Msg）
│
├── chat/                                   # 消息驱动协调机制
│   ├── MsgHub.java                         #   消息中心：广播、动态参与者管理（对应 MsgHub）
│   └── Pipeline.java                       #   工作流管线：顺序/扇出（对应 sequential/fanout_pipeline）
│
├── game/                                   # 三国狼人杀游戏逻辑
│   ├── GameRole.java                       #   游戏角色枚举（狼人/预言家/女巫/村民）
│   ├── Player.java                         #   玩家记录（三国人物 × 游戏角色 × 智能体）
│   ├── RolePrompts.java                    #   角色提示词工厂 + 结构化输出格式定义
│   └── ThreeKingdomsWerewolfGame.java      #   游戏控制器（夜晚/白天/投票流程）
│
└── runner/
    └── WerewolfGameRunner.java             #   CommandLineRunner 启动入口
```

## 核心业务流程

1. `WerewolfGameRunner.run()` 启动游戏
2. `RolePrompts.createDefaultPlayers()` 创建 6 名角色：孙权(狼人)、周瑜(狼人)、曹操(预言家)、张飞(女巫)、司马懿(村民)、赵云(村民)
3. `ThreeKingdomsWerewolfGame.run()` 进入游戏循环
4. 夜晚阶段：
   - 狼人通过 MsgHub 私密通信选择目标
   - 预言家点对点查验目标
   - 女巫决定使用药物
5. 白天阶段：
   - 全体通过 MsgHub 公开讨论
   - fanout_pipeline 并行投票
6. 胜负判定：好人全灭或狼人全灭

## AgentScope 概念映射

| AgentScope (Python) | 本模块 (Java) |
|---------------------|---------------|
| `Msg` | `Msg` (record) |
| `AgentBase` | `Agent` (interface) |
| `DialogAgent` | `DialogAgent` |
| `MsgHub` | `MsgHub` (AutoCloseable) |
| `sequential_pipeline` | `Pipeline.sequential()` |
| `fanout_pipeline` | `Pipeline.fanout()` |
| 结构化输出 (Pydantic) | JSON 格式提示 + 正则解析 |

## 设计文档

详见 [docs/design/agentscope-werewolf-game.md](../../docs/design/agentscope-werewolf-game.md)
