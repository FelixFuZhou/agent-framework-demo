# AGENTS.md — LangGraph Demo 模块上下文（L4 模块级）

## 模块简介

基于 LangGraph 框架核心理念，使用 Java 21 + Spring Boot 3.3 + Spring AI 1.0 实现状态图（StateGraph）驱动的工作流编排机制。

案例场景：三步问答助手——用户提问后依次经过"理解意图 → 搜索信息 → 生成答案"三个节点，每个节点读取并更新共享状态，形成完整的有向图执行流程。

## LangGraph 框架核心理念

LangGraph 是 LangChain 生态的扩展，将智能体流程建模为**有向图（Directed Graph）**：

1. **状态（State）**：贯穿图执行的共享数据结构，所有节点都能读写
2. **节点（Node）**：执行具体计算的函数，接收状态、返回更新后的状态
3. **边（Edge）**：定义节点间的跳转逻辑，分为常规边（固定跳转）和条件边（动态路由）
4. **条件边（Conditional Edge）**：根据当前状态动态决定下一步跳转目标，支持循环和分支

## 与 Python LangGraph 的对应关系

| Python LangGraph | Java 实现 | 说明 |
|---|---|---|
| `TypedDict` State | `GraphState` 类 | 共享状态对象（Map 存储） |
| Node 函数 | `Node` 接口 | `execute(state) → state` |
| `StateGraph` | `StateGraph` 类 | Builder 模式构建图 |
| `workflow.compile()` | `graph.compile()` → `CompiledGraph` | 编译后可执行 |
| `app.stream()` | `compiledGraph.stream()` | 流式执行 + 事件回调 |
| `add_node()` | `addNode()` | 注册节点 |
| `add_edge()` | `addEdge()` | 常规边 |
| `add_conditional_edges()` | `addConditionalEdges()` | 条件边 |
| `START` / `END` | `setEntryPoint()` / `StateGraph.END` | 入口/终止 |
| Tavily 搜索 | LLM 模拟搜索 | 不依赖外部 API |

## 目录结构

```
langgraph-demo/src/main/java/com/example/langgraph/
├── LangGraphDemoApplication.java        # Spring Boot 启动类
├── config/
│   └── LangGraphConfig.java             # Spring 配置
├── graph/
│   ├── GraphState.java                  # 全局状态（核心数据结构）
│   ├── Node.java                        # 节点接口
│   ├── ConditionalEdge.java             # 条件路由接口
│   └── StateGraph.java                  # 状态图 + CompiledGraph + NodeEvent
├── model/
│   └── SpringAIChatClient.java          # 模型客户端
├── node/
│   ├── UnderstandNode.java              # 理解节点（分析意图 + 生成搜索词）
│   ├── SearchNode.java                  # 搜索节点（LLM 模拟搜索）
│   └── AnswerNode.java                  # 回答节点（综合生成 + 降级策略）
└── runner/
    └── QAAssistantRunner.java           # 问答助手运行器（交互式 CLI）
```

## 核心机制：状态图执行流程

```
┌──────────────────────────────────────────────────┐
│                  StateGraph 状态图                 │
├──────────────────────────────────────────────────┤
│                                                  │
│  用户输入 "明天去北京，天气怎么样？"                │
│      ↓                                           │
│  [understand] 理解意图 + 生成搜索词                │
│      ↓ (常规边)                                   │
│  [search] 执行搜索                                │
│      ↓ (条件边)                                   │
│      ├── step="searched" → [answer] 基于结果回答   │
│      └── step="search_failed" → [answer] 降级回答  │
│      ↓                                           │
│  [END] 输出最终答案                               │
│                                                  │
└──────────────────────────────────────────────────┘
```

## 运行方式

```bash
cd langgraph-demo
mvn spring-boot:run
```

环境变量：
```bash
export DASHSCOPE_API_KEY=your-api-key
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_MODEL_ID=qwen-plus
```
