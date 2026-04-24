# LangGraph Demo — 三步问答助手

基于 [LangGraph](https://github.com/langchain-ai/langgraph) 框架核心理念的 Java 实现，展示**状态图（StateGraph）**驱动的工作流编排机制。

## 案例场景

构建一个遵循"理解 → 搜索 → 回答"三步流程的智能问答助手：

| 步骤 | 节点 | 职责 |
|------|------|------|
| 🧠 理解 | UnderstandNode | 分析用户意图，生成搜索关键词 |
| 🔍 搜索 | SearchNode | 执行信息搜索，获取知识要点 |
| 💡 回答 | AnswerNode | 综合搜索结果，生成最终答案 |

## LangGraph 核心理念

LangGraph 将智能体流程建模为**有向图**，三个核心概念：

### 1. 状态（State）
贯穿整个图的共享数据结构，所有节点都能读写：

```java
GraphState state = new GraphState()
    .put(GraphState.MESSAGES, "用户问题")
    .put(GraphState.STEP, "start");
```

### 2. 节点（Node）
执行具体计算的函数单元，接收状态、返回更新后的状态：

```java
public class UnderstandNode implements Node {
    public GraphState execute(GraphState state) {
        // 调用 LLM 分析意图
        // 更新 state 中的 user_query, search_query, step
        return state;
    }
}
```

### 3. 边（Edge）与条件边
定义节点间跳转逻辑，条件边支持动态路由：

```java
graph.addConditionalEdges("search",
    state -> "search_failed".equals(state.get("step")) ? "fallback" : "success",
    Map.of("success", "answer", "fallback", "answer")
);
```

### 图结构

```
START → [understand] → [search] → [条件路由] → [answer] → END
                                     ↓ success     ↓ fallback
                                  基于搜索结果   基于LLM知识
```

## 技术架构

```
com.example.langgraph/
├── graph/
│   ├── GraphState.java         # 全局状态（共享数据结构）
│   ├── Node.java               # 节点接口
│   ├── ConditionalEdge.java    # 条件路由接口
│   └── StateGraph.java         # 状态图引擎（Builder + CompiledGraph）
├── node/
│   ├── UnderstandNode.java     # 理解节点
│   ├── SearchNode.java         # 搜索节点
│   └── AnswerNode.java         # 回答节点
├── model/
│   └── SpringAIChatClient.java # Spring AI 模型客户端
├── config/
│   └── LangGraphConfig.java    # Spring 配置
└── runner/
    └── QAAssistantRunner.java  # 交互式问答运行器
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
cd langgraph-demo
mvn spring-boot:run
```

### 运行效果

```
╔══════════════════════════════════════════╗
║    LangGraph 智能问答助手启动            ║
║    三步流程：理解 → 搜索 → 回答          ║
╚══════════════════════════════════════════╝

🤔 您想了解什么: 什么是拖延症？如何克服？

════════════════════════════════════════════
🧠 理解阶段:
   理解：用户想了解拖延症的定义和克服方法
   搜索关键词: 拖延症定义 克服方法 心理学

🔍 搜索阶段: 搜索完成

💡 最终回答:
拖延症是一种自我调节失败的行为模式...
克服方法包括：
1. 番茄工作法...
2. 实施意图...
...
════════════════════════════════════════════
```

## 与 Python LangGraph 的对比

| 特性 | Python LangGraph | 本项目 Java 实现 |
|------|----------------|----------------|
| 语言 | Python 3.10+ | Java 21 |
| LLM | langchain_openai | Spring AI 1.0 |
| 状态定义 | TypedDict | GraphState (Map 封装) |
| 图构建 | StateGraph() | StateGraph (Builder) |
| 编译执行 | compile() → stream() | compile() → stream() |
| 搜索工具 | Tavily API | LLM 模拟搜索 |
| 交互模式 | CLI 循环 | CLI 循环 (Scanner) |
| Checkpointer | InMemorySaver | 未实现（保持简洁） |

## 框架设计对比（四大框架）

| 维度 | AutoGen | AgentScope | CAMEL | **LangGraph** |
|------|---------|------------|-------|-------------|
| 核心机制 | 对话驱动 + 轮询 | 消息驱动 + MsgHub | 角色扮演 + Inception | **状态图 + 条件边** |
| 流程控制 | 隐式（对话涌现） | 半显式（Pipeline） | 隐式（提示驱动） | **显式（图定义）** |
| 循环支持 | 通过对话轮次 | 通过消息重发 | 通过 step 循环 | **原生条件边** |
| 可控性 | 中等 | 高 | 低 | **最高** |
| 适用场景 | 团队协作 | 游戏/仿真 | 深度创作 | **复杂决策流** |
