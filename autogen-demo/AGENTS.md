# autogen-demo 模块上下文（L4 模块层）

## 模块概述

AutoGen 框架 Java 实现（基于 **Spring AI**）。案例：**软件开发团队**，4 个智能体通过 RoundRobinGroupChat 轮询协作完成软件开发任务。

使用 Spring AI `ChatClient` 替代手动 RestClient 调用 LLM，由框架统一管理模型配置。

## 文件结构与职责

```
com.example.autogen/
├── AutoGenDemoApplication.java          # Spring Boot 启动类
│
├── agent/                               # 智能体定义
│   ├── Agent.java                       #   接口：getName() + reply(chatHistory)
│   ├── AssistantAgent.java              #   LLM 驱动的助理智能体
│   └── UserProxyAgent.java              #   用户代理（交互/自动两种模式）
│
├── model/                               # LLM 模型客户端
│   └── SpringAIChatClient.java          #   封装 Spring AI ChatClient，统一 LLM 调用
│
├── message/                             # 消息
│   └── ChatMessage.java                 #   record: source + content + timestamp
│
├── chat/                                # 群聊协调机制
│   ├── RoundRobinGroupChat.java         #   轮询群聊：按顺序激活智能体
│   ├── TerminationCondition.java        #   终止条件接口
│   └── TextMentionTermination.java      #   关键词匹配终止
│
├── runner/                              # 启动入口
│   └── SoftwareDevTeamRunner.java       #   CommandLineRunner，组织协作流程
│
└── team/                                # 团队工厂
    └── SoftwareDevTeamFactory.java      #   创建四个角色的工厂方法
```

## 核心业务流程

1. `SoftwareDevTeamRunner.run()` 启动协作
2. `SoftwareDevTeamFactory` 创建 4 个智能体：ProductManager → Engineer → CodeReviewer → UserProxy
3. 将智能体注册到 `RoundRobinGroupChat`，设置 `TextMentionTermination("TERMINATE")`
4. 调用 `teamChat.run(task, callback)` 开始轮询
5. 每轮：当前智能体调用 `reply(chatHistory)` → 消息加入历史 → 检查终止条件 → 下一个

## 数据流

```
task(String) → ChatMessage("user", task)
                    ↓
        ProductManager.reply() → ChatMessage("ProductManager", ...)
                    ↓
        Engineer.reply()       → ChatMessage("Engineer", ...)
                    ↓
        CodeReviewer.reply()   → ChatMessage("CodeReviewer", ...)
                    ↓
        UserProxy.reply()      → ChatMessage("UserProxy", "...TERMINATE")
                    ↓
        TextMentionTermination.shouldTerminate() → true → 结束
```

## 设计文档

详见 [docs/design/autogen-software-dev-team.md](../../docs/design/autogen-software-dev-team.md)
