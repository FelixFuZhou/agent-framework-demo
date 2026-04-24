# LangGraph 三步问答助手案例设计文档

## 1. 业务目标

通过 LangGraph 的 StateGraph 机制，构建一个遵循"理解 → 搜索 → 回答"三步流程的智能问答助手。展示 LangGraph 框架**状态图驱动**的工作流编排方式和**条件边**的动态路由能力。

## 2. LangGraph 核心机制

### 2.1 状态图（StateGraph）

LangGraph 将执行流程建模为有向图，三要素：
- **State**：全局共享的数据结构，在节点间流动
- **Node**：执行计算的函数单元，读写 State
- **Edge**：节点间的跳转逻辑（常规边 + 条件边）

### 2.2 条件边（Conditional Edge）

条件边根据 State 中的字段值动态决定路由方向。在本案例中，搜索节点执行后根据 `step` 字段决定答案节点的执行策略。

## 3. 关键组件

### 3.1 GraphState

共享状态对象，包含字段：
- `messages`：消息历史
- `user_query`：LLM 分析后的用户意图
- `search_query`：优化后的搜索关键词
- `search_results`：搜索结果
- `final_answer`：最终答案
- `step`：当前步骤标记（start / understood / searched / search_failed / completed）

### 3.2 Node 接口

```java
@FunctionalInterface
public interface Node {
    GraphState execute(GraphState state);
}
```

### 3.3 三个节点实现

| 节点 | 输入状态 | 处理逻辑 | 输出状态 |
|------|---------|---------|---------|
| UnderstandNode | messages (用户原文) | LLM 分析意图 + 生成搜索词 | user_query, search_query, step="understood" |
| SearchNode | search_query | LLM 模拟搜索 | search_results, step="searched" 或 "search_failed" |
| AnswerNode | user_query, search_results, step | 根据 step 选择策略生成答案 | final_answer, step="completed" |

### 3.4 StateGraph 构建

```java
graph.setEntryPoint("understand");
graph.addEdge("understand", "search");          // 常规边
graph.addConditionalEdges("search", ...);       // 条件边
graph.addEdge("answer", StateGraph.END);        // 终止
```

## 4. 执行流程

```
用户输入："什么是量子计算？"
    ↓ [START]
[understand]
  ├── LLM 分析意图："用户想了解量子计算的基本概念和原理"
  └── 生成搜索词："量子计算 基本原理 概念"
    ↓ (常规边)
[search]
  ├── 成功：LLM 返回5条知识要点 → step="searched"
  └── 失败：捕获异常 → step="search_failed"
    ↓ (条件边 → 都路由到 answer)
[answer]
  ├── step="searched" → 基于搜索结果综合生成答案
  └── step="search_failed" → 回退到 LLM 内部知识回答
    ↓ (常规边)
[END] → 输出最终答案
```

## 5. 与 Python LangGraph 的差异

| 方面 | Python LangGraph | Java 实现 |
|------|----------------|----------|
| 搜索 | Tavily API 真实搜索 | LLM 模拟（不依赖外部 API） |
| State | TypedDict + Annotated | GraphState (Map 封装) |
| Checkpointer | InMemorySaver | 未实现 |
| 消息 | langchain Messages | 简单字符串拼接 |
| 交互 | input() 循环 | Scanner 循环 |

## 6. 设计决策

1. **LLM 模拟搜索 vs 真实搜索 API**：选择 LLM 模拟，避免依赖外部 API 密钥，让案例可以开箱即用。核心目的是展示 StateGraph 机制而非搜索能力。
2. **Map vs Record State**：选择 Map 以贴近 Python 的 TypedDict 语义，保持灵活性。
3. **交互式 CLI**：支持多轮对话，更贴近教程中的用户体验。
4. **条件边在搜索后**：即使两个分支都指向 answer 节点，仍然使用条件边，目的是展示条件路由的机制（answer 节点内部根据 step 字段选择不同策略）。
