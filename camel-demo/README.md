# CAMEL Demo — AI 科普电子书角色扮演协作

基于 [CAMEL](https://github.com/camel-ai/camel) 框架核心理念的 Java 实现，展示 **Role-Playing（角色扮演）** 和 **Inception Prompting（启始提示）** 机制。

## 案例场景

两个 AI 智能体自主协作生成一本关于**"拖延症心理学"**的科普电子书：

| 角色 | 智能体类型 | 职责 |
|------|-----------|------|
| 🧠 心理学家 | AI Assistant | 提供专业心理学知识，根据指示撰写内容 |
| ✍️ 科普作家 | AI User | 规划电子书结构，逐步指示心理学家完成各章节 |

## CAMEL 核心理念

CAMEL（Communicative Agents for "Mind" Exploration of Large Language Model Society）的核心创新：

### Inception Prompting

通过精心设计的系统提示词，让 AI 自发进入角色并自主协作：

```
AI Assistant 的视角：
"永远不要忘记你是心理学家，我是科普作家。永远不要颠倒角色！
 你必须帮助我完成任务...
 你应该始终以 '解决方案：' 开始回复"

AI User 的视角：
"永远不要忘记你是科普作家，我是心理学家。永远不要颠倒角色！
 你总是会指示我...
 你必须使用 '指示：' 的格式"
```

### 协作流程

```
Task Specifier（细化任务）
    ↓
AI User 发出第一条指示
    ↓
┌──────── 协作循环 ────────┐
│  AI User → 指示 → AI Assistant  │
│  AI User ← 解决方案 ← AI Assistant  │
│  AI User 评估 → 下一条指示        │
└─────────────────────────────────┘
    ↓
AI User 判断完成 → <CAMEL_TASK_DONE>
```

## 技术架构

```
com.example.camel/
├── agent/
│   ├── ChatAgent.java          # 智能体接口（step 单步对话）
│   └── CamelAgent.java         # 实现（独立记忆 + 视角翻转）
├── chat/
│   ├── InceptionPrompts.java   # Inception Prompt 模板（核心创新）
│   └── RolePlaying.java        # 角色扮演协调器
├── config/
│   └── CamelConfig.java        # Spring 配置
├── message/
│   └── ChatMessage.java        # 消息 record
├── model/
│   └── SpringAIChatClient.java # Spring AI 模型客户端
└── runner/
    └── EbookRunner.java        # 运行入口
```

## 快速开始

### 1. 设置环境变量

```bash
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_MODEL_ID=qwen-plus
```

### 2. 编译运行

```bash
cd camel-demo
mvn spring-boot:run
```

### 运行效果示例

```
╔══════════════════════════════════════════╗
║       CAMEL 角色扮演协作系统启动         ║
╚══════════════════════════════════════════╝

AI Assistant（执行者）: 心理学家
AI User（指挥者）: 科普作家
任务: 写一本关于拖延症心理学的科普电子书...

【科普作家 的第一条指示】
指示：请撰写第一章"什么是拖延症"，包括拖延症的科学定义...

========== 第 1 轮对话 ==========

【心理学家 的解决方案】
解决方案：拖延症（Procrastination）是一种自我调节失败的行为模式...

【科普作家 的下一条指示】
指示：请撰写第二章"拖延的心理成因"...

...（多轮协作）...

╔══════════════════════════════════════════╗
║         任务完成！协作结束               ║
╚══════════════════════════════════════════╝
```

## 与 Python CAMEL 的对比

| 特性 | Python CAMEL | 本项目 Java 实现 |
|------|-------------|----------------|
| 语言 | Python 3.10+ | Java 21 |
| LLM SDK | OpenAI Python / 自研 | Spring AI 1.0 |
| Agent 基类 | `ChatAgent` | `ChatAgent` 接口 + `CamelAgent` |
| 协调机制 | `RolePlaying` | `RolePlaying`（等价实现） |
| 提示词 | 内置英文模板 | 中文 Inception Prompt |
| 终止信号 | `<CAMEL_TASK_DONE>` | 相同 |
| 任务细化 | `TaskSpecifyAgent` | `InceptionPrompts.taskSpecifier()` |

## 框架设计对比（四大框架）

| 维度 | AutoGen | AgentScope | **CAMEL** | LangGraph |
|------|---------|------------|-----------|-----------|
| 核心机制 | 对话驱动 + 轮询 | 消息驱动 + MsgHub | **角色扮演 + Inception Prompt** | 状态图 + 条件边 |
| 智能体数量 | 多智能体 | 多智能体 | **双智能体** | 可变 |
| 人类参与 | 可选 | 可选 | **完全自主** | 可选 |
| 架构重点 | 协调基础设施 | 消息路由 | **提示词工程** | 流程编排 |
| 适用场景 | 团队协作 | 游戏/仿真 | **深度创作/研究** | 复杂决策流 |
