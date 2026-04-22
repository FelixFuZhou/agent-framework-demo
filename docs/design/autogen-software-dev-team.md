# AutoGen 软件开发团队 — 设计文档

> 对应原文：[第六章 6.2 框架一：AutoGen](https://datawhalechina.github.io/hello-agents/#/./chapter6/第六章%20框架开发实践) 中的"软件开发团队"案例

## 1. 业务目标

用 Java 实现 AutoGen 框架的核心机制，通过一个**模拟软件开发团队**的案例来演示多智能体协作。团队协作完成一个比特币价格展示应用的需求分析、编码、审查和验收。

## 2. 核心机制

AutoGen 的设计哲学：**以对话驱动协作**。

将多智能体系统抽象为一个由多个"可对话"智能体组成的群聊，智能体按照预设顺序发言，通过消息传递完成协作。

### 关键组件

| 组件 | 职责 | Java 对应 |
|------|------|--------|
| AssistantAgent | 基于 LLM 的智能体，通过 System Message 定义专家角色 | `AssistantAgent` 类 |
| UserProxyAgent | 用户代理，发起任务 + 发出 TERMINATE 终止 | `UserProxyAgent` 类 |
| RoundRobinGroupChat | 轮询群聊协调器，按顺序激活智能体 | `RoundRobinGroupChat` 类 |
| TextMentionTermination | 终止条件，检测消息中的关键词 | `TextMentionTermination` 类 |
| OpenAIChatCompletionClient | 兼容 OpenAI API 的模型客户端 | `SpringAIChatClient`（封装 Spring AI `ChatClient`） |

## 3. 智能体角色设计

```
ProductManager（产品经理）
    ↓ "请工程师开始实现"
Engineer（工程师）
    ↓ "请代码审查员检查"
CodeReviewer（代码审查员）
    ↓ "代码审查完成，请用户代理测试"
UserProxy（用户代理）
    ↓ "TERMINATE" → 终止
```

| 角色 | 类型 | 职责 | 关键 System Message 指令 |
|------|------|------|--------------------------|
| ProductManager | AssistantAgent | 需求分析、功能划分、技术选型 | 完成后说"请工程师开始实现" |
| Engineer | AssistantAgent | 编码实现，提供完整 Java 代码 | 完成后说"请代码审查员检查" |
| CodeReviewer | AssistantAgent | 代码质量审查、安全性检查 | 完成后说"代码审查完成，请用户代理测试" |
| UserProxy | UserProxyAgent | 代表用户验收，发出终止信号 | 完成测试后回复 TERMINATE |

## 4. 协作流程

```
1. 用户任务 (task) 作为初始消息写入对话历史
2. RoundRobinGroupChat 按 participants 列表顺序轮询
3. 当前智能体读取完整对话历史，调用 LLM 生成回复
4. 回复写入对话历史，检查 TerminationCondition
5. 若未终止，轮到下一个智能体；若终止或达到 maxTurns，结束
```

## 5. 技术决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| LLM 调用方式 | Spring AI `ChatClient` | 学习项目，拥抱 Spring 生态；Spring AI 自动配置 + 统一抽象 |
| UserProxy 模式 | 支持交互/自动两种 | 交互模式用于真实使用，自动模式用于演示和测试 |
| 消息格式 | `record ChatMessage` | 不可变，简洁，Java 21 特性 |
| 角色创建 | 工厂模式 `SoftwareDevTeamFactory` | 解耦角色定义和协作流程，便于复用 |

## 6. 与 Python 原版的差异

| 方面 | Python AutoGen 0.7.4 | Java 实现 |
|------|----------------------|-----------|
| 异步模型 | async/await 全异步 | 同步调用（简化） |
| 框架 | autogen-agentchat 库 | Spring AI `ChatClient` + 手动实现核心抽象 |
| 配置方式 | 环境变量 + 函数 | Spring AI 自动配置 + application.properties |
| 运行入口 | asyncio.run() | CommandLineRunner |
| 流式输出 | Console(stream) | Consumer<ChatMessage> 回调 |
