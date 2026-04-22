# AGENTS.md — 项目全局上下文（L1 通用层）

## 项目简介

AI Agent 框架学习项目：基于 Java 语言实现 AutoGen、AgentScope、CAMEL、LangGraph 四大主流智能体框架的核心案例，用于理解和对比不同框架的设计理念与协作模式。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.3.5 |
| 构建工具 | Maven | 3.6+ |
| AI 框架 | Spring AI | 1.0.0 |
| LLM API | OpenAI 兼容接口（通过 Spring AI） | — |

## 项目目录结构

```
agent-framework-demo/               # 父工程（Maven 多模块）
├── pom.xml                          # 父 POM，统一管理依赖版本
├── AGENTS.md                        # 本文件：L1 项目全局上下文
├── README.md                        # 项目说明文档
│
├── docs/
│   ├── conventions/                 # L2 编码规范
│   │   └── backend.md              #   Java 后端编码规范
│   └── design/                      # L3 设计文档
│       └── autogen-software-dev-team.md  # AutoGen 软件开发团队案例设计
│
├── .cursor/rules/                   # 工具优化层（Cursor 专属）
│   └── backend-conventions.mdc      #   Java 文件编辑时自动注入的精简规范
│
├── .github/
│   └── copilot-instructions.md      # GitHub Copilot 指令文件
│
├── autogen-demo/                    # 模块1：AutoGen 框架实现（基于 Spring AI）
│   ├── pom.xml
│   ├── AGENTS.md                    # L4 模块级上下文
│   └── src/main/java/com/example/autogen/
│
├── agentscope-demo/                 # 模块2：AgentScope 框架实现（待开发）
│   ├── pom.xml
│   └── src/
│
├── camel-demo/                      # 模块3：CAMEL 框架实现（待开发）
│   ├── pom.xml
│   └── src/
│
└── langgraph-demo/                  # 模块4：LangGraph 框架实现（待开发）
    ├── pom.xml
    └── src/
```

## 运行环境

- **JDK**：21（本机路径 `/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`）
- **构建**：`mvn compile` / `mvn spring-boot:run`（需在子模块目录执行）
- **LLM 配置**：通过环境变量注入，见下方说明

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export LLM_API_KEY=your-api-key
export LLM_BASE_URL=https://api.deepseek.com/v1   # 或其他 OpenAI 兼容接口
export LLM_MODEL_ID=deepseek-chat                  # 模型名称
```

## 核心领域名词表

| 名词 | 说明 |
|------|------|
| Agent（智能体） | 能够根据上下文进行"思考"并产生行为的基本单元 |
| AssistantAgent | 基于 LLM 的智能体，通过 System Message 定义角色和职责 |
| UserProxyAgent | 用户代理智能体，不依赖 LLM，代表人类用户发起任务和验收 |
| ModelClient | 模型客户端接口，封装与 LLM API 的交互 |
| RoundRobinGroupChat | 轮询群聊，按预定义顺序让智能体依次发言的协调机制 |
| TerminationCondition | 终止条件，控制群聊何时结束（如检测到 TERMINATE 关键词） |
| ChatMessage | 消息对象，智能体之间通信的基本单元 |
| MsgHub | 消息中心（AgentScope），负责消息路由和分发 |
| RolePlaying | 角色扮演（CAMEL），双智能体自主协作的核心机制 |
| StateGraph | 状态图（LangGraph），将智能体流程建模为有向图 |
| Node | 节点（LangGraph），图中执行具体计算的步骤 |
| Edge | 边（LangGraph），定义节点之间的跳转逻辑 |

## 四个框架的案例对照

| 模块 | 框架 | 案例 | 核心机制 | 状态 |
|------|------|------|----------|------|
| autogen-demo | AutoGen | 软件开发团队 | 对话驱动 + 轮询群聊 | ✅ 已实现 |
| agentscope-demo | AgentScope | 三国狼人杀游戏 | 消息驱动 + MsgHub | 📌 待开发 |
| camel-demo | CAMEL | AI 科普电子书 | 角色扮演 + 引导性提示 | 📌 待开发 |
| langgraph-demo | LangGraph | 三步问答助手 | 状态图 + 条件边 | 📌 待开发 |

## 统一 LLM 配置规范

每个模块通过 `application.properties` 配置 LLM 连接参数，统一使用以下三个配置项：

| 配置项 | 环境变量 | 说明 |
|--------|----------|------|
| `spring.ai.openai.chat.options.model` | `LLM_MODEL_ID` | 模型名称 |
| `spring.ai.openai.api-key` | `LLM_API_KEY` / `DASHSCOPE_API_KEY` | API 密钥 |
| `spring.ai.openai.base-url` | `LLM_BASE_URL` | API 基础地址 |

## 编码规范索引

- 后端 Java 编码规范：[docs/conventions/backend.md](docs/conventions/backend.md)

## 设计文档索引

- AutoGen 软件开发团队案例：[docs/design/autogen-software-dev-team.md](docs/design/autogen-software-dev-team.md)
