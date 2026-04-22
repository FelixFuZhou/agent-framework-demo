# Agent Framework Demo

基于 Java 语言实现四大主流 AI Agent 框架的核心案例，用于学习和对比不同框架的设计理念与多智能体协作模式。

> 案例来源：[Datawhale Hello-Agents 第六章 框架开发实践](https://datawhalechina.github.io/hello-agents/#/./chapter6/第六章%20框架开发实践)

## 技术栈

- **Java 21** — record、text block、pattern matching
- **Spring Boot 3.3.x** — 依赖注入、配置管理、RestClient
- **Maven** — 多模块构建
- **LLM** — 任何 OpenAI 兼容 API（OpenAI / DeepSeek / 通义千问 / Ollama）

## 项目结构

```
agent-framework-demo/
├── autogen-demo/        ✅ AutoGen   — 软件开发团队（对话驱动 + 轮询群聊）
├── agentscope-demo/     📌 AgentScope — 三国狼人杀游戏（消息驱动 + MsgHub）
├── camel-demo/          📌 CAMEL      — AI科普电子书（角色扮演 + 引导性提示）
└── langgraph-demo/      📌 LangGraph  — 三步问答助手（状态图 + 条件边）
```

## 四大框架对比

| 框架 | 核心理念 | 协作模式 | 案例 |
|------|----------|----------|------|
| **AutoGen** | 以对话驱动协作 | 轮询群聊（RoundRobinGroupChat） | 4 个角色协作开发比特币价格应用 |
| **AgentScope** | 工程化优先的消息驱动 | MsgHub 消息中心 + Pipeline | 三国人物的狼人杀游戏 |
| **CAMEL** | 角色扮演自主协作 | 双智能体 RolePlaying | 心理学家和作家合写电子书 |
| **LangGraph** | 状态图显式控制流 | 有向图 Node + Edge | 理解→搜索→回答三步问答 |

## 快速开始

### 前置条件

- JDK 21
- Maven 3.6+
- 一个 LLM API Key（OpenAI / DeepSeek / 通义千问 等）

### 设置环境变量

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export LLM_API_KEY=your-api-key
export LLM_BASE_URL=https://api.deepseek.com/v1
export LLM_MODEL_ID=deepseek-chat
```

### 编译项目

```bash
mvn compile
```

### 运行 AutoGen Demo

```bash
cd autogen-demo
mvn spring-boot:run
```

运行后会看到 4 个智能体的协作流程：

```
🔧 正在初始化模型客户端...
👥 正在创建智能体团队...
🚀 启动 AutoGen 软件开发团队协作...
============================================================
---------- TextMessage (user) ----------
我们需要开发一个比特币价格显示应用...

---------- TextMessage (ProductManager) ----------
### 1. 需求理解与分析 ...
请工程师开始实现。

---------- TextMessage (Engineer) ----------
### 技术方案实施 ...
请代码审查员检查。

---------- TextMessage (CodeReviewer) ----------
### 代码审查 ...
代码审查完成，请用户代理测试。

---------- TextMessage (UserProxy) ----------
已经完成需求审查，代码看起来没问题。TERMINATE
============================================================
✅ 团队协作完成！
```

## AI 辅助开发上下文结构

本项目遵循 [AI 辅助开发协作规范](https://www.yuque.com/u12172133/gx486v/tqlmx5lc5pylpklg)，分层管理 AI 上下文：

| 层级 | 文件 | 说明 |
|------|------|------|
| L1 通用层 | `AGENTS.md` | 项目架构、技术栈、名词表、规范索引 |
| L2 规范层 | `docs/conventions/backend.md` | Java 后端编码规范 |
| L3 设计层 | `docs/design/*.md` | 各框架案例的设计文档 |
| L4 模块层 | `{module}/AGENTS.md` | 模块级业务逻辑说明 |
| Cursor | `.cursor/rules/*.mdc` | 精简规范，编辑 Java 文件时自动注入 |
| Copilot | `.github/copilot-instructions.md` | GitHub Copilot 指令 |

## License

本项目仅供学习用途。