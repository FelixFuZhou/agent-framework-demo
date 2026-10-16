# Agent Framework Demo

**用 Java 理解 AI Agent：从角色、消息和记忆，到多智能体协作与状态图编排。**

这是一个面向 Java 开发者的 AI Agent 框架学习项目。基于 **Java 21 + Spring Boot + Spring AI**，通过软件开发团队、三国狼人杀、科普电子书和问答助手四个案例，动手理解 AutoGen、AgentScope、CAMEL、LangGraph 的核心设计思路。

我把学习过程中的代码、设计文档和思考整理在这里，希望帮助更多开发者读懂 Agent 的协作机制，并把这些思路用到自己的项目中。欢迎一起学习、讨论和完善。

> 案例参考：[Datawhale Hello-Agents 第六章：框架开发实践](https://datawhalechina.github.io/hello-agents/#/./chapter6/第六章%20框架开发实践)。本仓库使用 Spring AI 调用模型，并以 Java 编写上述框架核心机制的教学简化实现；覆盖范围以各模块源码为准。

## 适合谁学习

- 熟悉 Java / Spring Boot，希望开始学习 AI Agent 的后端开发者。
- 已经会调用大模型 API，希望理解多轮任务、角色分工和流程控制如何实现的同学。
- 想结合代码比较不同 Agent 编排方式，或寻找技术分享、课程实验素材的学习者。

阅读代码需要基本的 Java、Maven 和 HTTP API 知识。可以从任意一个案例开始，逐步对照其他模块。

## 你能从这个项目学到什么

### 1. 理解一个 Agent 由哪些部分组成

从代码中拆解 **角色提示词、模型调用、对话历史和行为接口** 的关系：角色如何通过 System Prompt 定义，历史消息如何进入上下文，一次模型回复又如何成为下一步协作的输入。

你会接触到 `Agent`、`ChatMessage` / `Msg`、`SpringAIChatClient` 等基础抽象，并理解：同一个模型客户端，如何通过不同的角色定义和上下文，参与不同任务。

### 2. 理解多个 Agent 如何协作

四个案例分别展示四种组织方式：

| 模块 | 案例 | 本项目展示的协作机制 | 学习重点 |
|------|------|----------------------|----------|
| [AutoGen](autogen-demo/README.md) | 软件开发团队 | 多角色轮询群聊 | 角色分工、共享历史、人工介入、终止条件 |
| [AgentScope](agentscope-demo/README.md) | 三国狼人杀 | MsgHub 消息分发 + Pipeline | 消息广播、私密频道、独立记忆、讨论与投票 |
| [CAMEL](camel-demo/README.md) | 拖延症心理学科普电子书 | AI User 与 AI Assistant 双角色协作 | 任务细化、Inception Prompting、视角转换、自主推进 |
| [LangGraph](langgraph-demo/README.md) | 理解 → 搜索 → 回答的问答助手 | 共享状态 + 节点 + 条件边 | 任务拆解、状态传递、显式路由、失败降级 |

对照阅读时，可以重点观察三个问题：**谁决定下一步做什么？谁能看到哪些消息？任务何时结束？** 这些问题能帮助你把框架名词和实际代码联系起来。

### 3. 理解上下文与流程控制如何影响结果

- **共享历史与独立记忆**：AutoGen 中的团队共享对话历史，CAMEL 中的两个角色分别维护自己的记忆；AgentScope 通过消息分发决定各角色接收什么信息。
- **角色与消息视角**：同一条消息，在发送者和接收者的上下文中承担什么角色？CAMEL 的 `CamelAgent` 展示了这种转换。
- **结束与兜底机制**：通过 `TERMINATE`、`<CAMEL_TASK_DONE>`、最大轮次或最大执行步数控制协作结束。
- **模型输出与程序规则的衔接**：狼人杀通过格式提示约束决策输出，再由 Java 代码解析、结算；问答助手根据状态选择正常回答或降级回答。

### 4. 学习把 Agent 协作接入 Java Web 应用

AutoGen 模块同时提供命令行和 Web 交互，可以继续学习：

- 使用 Spring AI `ChatClient` 封装模型调用，通过环境变量配置模型连接。
- 把协作循环拆成单步执行，并将会话状态保存到 Redis。
- 在轮到用户时暂停执行，提交用户回复后继续推进任务。
- 使用 SSE 展示各角色的发言，通过 Redis Pub/Sub 分发事件。
- 从对话中的 Markdown 代码块提取文件，将生成内容写入本地工程目录。

### 5. 形成自己的编排判断

结合四个案例，尝试判断：一个任务需要多角色讨论、按范围分发消息、双角色持续创作，还是预先定义每一步的执行路径？

各模块附有 `框架理念思考.md`，整理了设计取舍、适用场景讨论和自测问题。可以先运行、再读源码，最后带着自己的实验结果回看这些思考。

## 四个案例怎么读

### AutoGen：多角色怎样接力完成一个任务

```text
用户任务 → 产品经理 → 工程师 → 代码审查员 → 用户代理 → 继续迭代或结束
```

从 [SoftwareDevTeamFactory](autogen-demo/src/main/java/com/example/autogen/team/SoftwareDevTeamFactory.java) 看角色定义，再读 [RoundRobinGroupChat](autogen-demo/src/main/java/com/example/autogen/chat/RoundRobinGroupChat.java) 的发言顺序、历史传递和终止判断。

想进一步理解 Web 交互，可以沿着 [TeamChatService](autogen-demo/src/main/java/com/example/autogen/service/TeamChatService.java) 和 [ChatSessionState](autogen-demo/src/main/java/com/example/autogen/chat/ChatSessionState.java) 跟踪一次“执行 → 等待用户 → 继续执行”的状态变化。

### AgentScope：谁应该收到这条消息

```text
夜晚私密协商 / 点对点行动 → 白天公开讨论 → 独立投票 → 结算与胜负判断
```

从 [MsgHub](agentscope-demo/src/main/java/com/example/agentscope/chat/MsgHub.java) 的 `broadcast()`、`speak()` 和 Agent 的 `reply()` / `observe()` 入手，理解主动回复与被动接收的区别。

再读 [Pipeline](agentscope-demo/src/main/java/com/example/agentscope/chat/Pipeline.java) 与 [ThreeKingdomsWerewolfGame](agentscope-demo/src/main/java/com/example/agentscope/game/ThreeKingdomsWerewolfGame.java)，观察游戏规则如何组织消息流。当前 `fanout()` 逐个调用 Agent，展示的是向多个角色收集独立回复的同步实现。

### CAMEL：让一个 AI 发出指示，另一个 AI 执行

```text
任务细化 → 科普作家发出指示 → 心理学家提供内容 → 作家给出下一步指示 → 结束
```

先读 [InceptionPrompts](camel-demo/src/main/java/com/example/camel/chat/InceptionPrompts.java)，理解如何用提示词约定角色、任务和回复方式；再读 [CamelAgent](camel-demo/src/main/java/com/example/camel/agent/CamelAgent.java) 的独立记忆和消息视角转换。

最后读 [RolePlaying](camel-demo/src/main/java/com/example/camel/chat/RolePlaying.java)，跟踪任务细化、双角色交替执行、内容汇总和完成信号。

### LangGraph：把任务流程写成一张图

```text
理解问题 → 模拟搜索 → 条件判断 → 回答 → END
                       ├─ 成功：根据搜索结果回答
                       └─ 失败：使用模型已有知识回答
```

从 [QAAssistantRunner](langgraph-demo/src/main/java/com/example/langgraph/runner/QAAssistantRunner.java) 看图的组装，再读 [GraphState](langgraph-demo/src/main/java/com/example/langgraph/graph/GraphState.java)、[StateGraph](langgraph-demo/src/main/java/com/example/langgraph/graph/StateGraph.java) 和 [各节点实现](langgraph-demo/src/main/java/com/example/langgraph/node)，理解状态如何贯穿执行过程。

当前搜索节点由 LLM 模拟知识搜索，适合观察状态传递和异常处理。成功与失败两条路由都进入 `AnswerNode`，由该节点根据状态选择回答策略；可以在此基础上练习接入真实搜索工具或增加新分支。

## 技术栈与项目结构

| 技术 | 本项目中的用途 |
|------|----------------|
| Java 21 | Agent 接口、record 消息与状态、流程编排 |
| Spring Boot 3.3.5 | 应用启动、依赖注入、配置管理与 Web 接口 |
| Spring AI 1.0.0 | `ChatClient` 模型调用与 OpenAI 兼容接口接入 |
| Maven 3.6+ | 多模块依赖管理与构建 |
| Redis | AutoGen 会话状态存储与 Pub/Sub 事件分发 |

```text
agent-framework-demo/
├── pom.xml                  # 父工程，统一管理版本
├── autogen-demo/            # 软件开发团队，含 Web UI / SSE / Redis
├── agentscope-demo/         # 三国狼人杀，消息分发与游戏流程
├── camel-demo/              # 双角色协作，生成科普电子书内容
├── langgraph-demo/          # 三步问答，状态图与条件路由
├── docs/
│   ├── conventions/         # Java 后端编码规范
│   └── design/              # 案例设计文档
└── AGENTS.md                # 项目全局上下文与规范索引
```

四个模块均已包含案例实现，每个模块提供独立的 README、源码和框架理念思考文档。

## 快速开始

### 1. 准备环境并配置模型

需要 JDK 21、Maven 3.6+，以及可用的模型 API Key。确保 `java -version` 显示 Java 21；如有多个 JDK，请将 `JAVA_HOME` 指向自己的 JDK 21 安装目录。

以下示例与仓库默认的通义千问配置一致：

```bash
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode
export LLM_MODEL_ID=qwen-turbo
```

当前各模块的 `application.properties` 读取 **`DASHSCOPE_API_KEY`** 作为密钥变量。切换其他 OpenAI 兼容服务时，请一并设置该变量、对应的 API 基础地址和模型名称。

### 2. 编译项目

以下命令均在项目根目录执行：

```bash
mvn compile
```

### 3. 选择一个案例运行

初次阅读可以先运行 LangGraph，跟踪三个节点的输入、状态变化和最终输出：

```bash
mvn spring-boot:run -pl langgraph-demo
```

在控制台输入问题开始体验，输入 `quit` 或 `exit` 退出。其他案例的启动方式：

```bash
# 三国狼人杀：观察多角色讨论、行动和投票
mvn spring-boot:run -pl agentscope-demo

# 科普电子书：观察双角色交替协作
mvn spring-boot:run -pl camel-demo
```

CAMEL 当前在控制台输出内容，并尝试保存到 `EbookRunner` 中指定的路径。该路径仍是作者本机路径 `/Users/felix/Desktop/book.txt`，运行前可将其改为自己的可写路径。

### 4. 体验 AutoGen Web 协作

AutoGen 模块需要 Redis。可以连接已有实例，并通过环境变量提供连接信息：

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password

mvn spring-boot:run -pl autogen-demo
```

Redis 未设置密码时，将 `REDIS_PASSWORD` 设为空字符串：`export REDIS_PASSWORD=''`。

启动后访问 [http://localhost:8080](http://localhost:8080)，输入任务，观察各角色发言，并在轮到用户时提交反馈。命令行自动演示模式可通过以下命令启动，当前启动配置同样需要 Redis：

```bash
AUTOGEN_MODE=cli mvn spring-boot:run -pl autogen-demo
```

更多配置、接口和运行说明见各模块 README。

## 建议的学习路线与动手练习

1. **先跟踪一次执行**：从 LangGraph 开始，观察同一份状态如何经过理解、搜索、回答三个节点。
2. **再理解角色与记忆**：阅读 CAMEL，修改角色或任务，观察提示词和独立上下文如何影响协作。
3. **扩展到多角色协作**：阅读 AutoGen，比较自动用户代理与 Web 人工反馈的差异，跟踪暂停和继续执行的过程。
4. **研究消息可见范围**：阅读 AgentScope，比较夜晚私密交流和白天公开讨论，检查每个 Agent 接收到了哪些消息。
5. **做一次自己的改造**：为问答助手接入真实搜索工具，给状态图增加审核节点，或为团队协作设计新的角色与终止规则。

实验时可以记录角色数量、对话轮次、响应耗时和输出质量，思考增加 Agent 或对话轮次是否带来了实际收益。

## 文档导航

| 模块 | 使用说明 | 设计文档 | 框架理念与自测问题 |
|------|----------|----------|--------------------|
| AutoGen | [README](autogen-demo/README.md) | [软件开发团队设计](docs/design/autogen-software-dev-team.md) | [框架理念思考](autogen-demo/框架理念思考.md) |
| AgentScope | [README](agentscope-demo/README.md) | — | [框架理念思考](agentscope-demo/框架理念思考.md) |
| CAMEL | [README](camel-demo/README.md) | [电子书协作设计](docs/design/camel-ebook-roleplaying.md) | [框架理念思考](camel-demo/框架理念思考.md) |
| LangGraph | [README](langgraph-demo/README.md) | [问答助手设计](docs/design/langgraph-qa-assistant.md) | [框架理念思考](langgraph-demo/框架理念思考.md) |

### AI 辅助开发上下文结构

项目还保留了 [AI 辅助开发协作规范](https://www.yuque.com/u12172133/gx486v/tqlmx5lc5pylpklg) 对应的分层上下文文件，可以作为组织项目知识的参考：

| 层级 | 文件 | 说明 |
|------|------|------|
| L1 通用层 | `AGENTS.md` | 项目架构、技术栈、名词表、规范索引 |
| L2 规范层 | [backend.md](docs/conventions/backend.md) | Java 后端编码规范 |
| L3 设计层 | `docs/design/*.md` | 案例设计文档 |
| L4 模块层 | `{module}/AGENTS.md` | 模块级业务逻辑说明 |
| Cursor | `.cursor/rules/*.mdc` | 编辑 Java 文件时注入的精简规范 |
| Copilot | `.github/copilot-instructions.md` | GitHub Copilot 指令 |

## 一起学习与完善

欢迎通过 Issue 分享运行中遇到的问题、对设计的疑问或自己的实验结果，也欢迎通过 PR 补充文档、修复问题和增加教学案例。

提交运行问题时，请说明模块名称、JDK 版本、模型名称、复现步骤及脱敏后的报错信息，方便大家一起定位。

## 使用说明

本项目仅供学习用途。欢迎结合示例修改角色、消息规则和执行流程，分享自己的学习记录与实验结果。
