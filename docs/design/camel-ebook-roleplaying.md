# CAMEL AI 科普电子书案例设计文档

## 1. 业务目标

通过 CAMEL 的 Role-Playing 机制，让两个 AI 智能体（心理学家 + 科普作家）自主协作完成一本关于"拖延症心理学"的科普电子书。展示 CAMEL 框架 **Inception Prompting** 的核心技术原理和**双智能体自主协作**的实现方式。

## 2. CAMEL 核心机制

### 2.1 Inception Prompting（启始提示）

CAMEL 论文（Li et al., 2023）的核心贡献。通过精心设计的系统提示词实现：

1. **角色锁定**：明确声明"你是 X，我是 Y，永远不要颠倒角色"
2. **任务锚定**：反复强调任务目标，防止对话偏离
3. **格式约束**：AI Assistant 必须以"解决方案："开头，AI User 必须以"指示："开头
4. **终止机制**：AI User 判断任务完成时输出 `<CAMEL_TASK_DONE>`
5. **视角翻转**：每个智能体的 system prompt 中"我"和"你"互换

### 2.2 Role-Playing（角色扮演）

双智能体协作模式：
- **AI User**（科普作家）：任务的指挥者，负责分解任务、发出指示、评估结果
- **AI Assistant**（心理学家）：任务的执行者，负责根据指示提供专业内容

### 2.3 Task Specifier（任务细化器）

可选步骤：将粗粒度的任务描述（"写一本电子书"）细化为具体可执行的方案（"按6章结构撰写，每章包含..."）。

## 3. 关键组件

### 3.1 ChatMessage（消息）

```java
public record ChatMessage(String role, String content, Instant timestamp)
```

与 AutoGen 的 `ChatMessage(source, content)` 不同，CAMEL 使用 `role` 而非 `source`，因为每个智能体维护独立的对话历史，需要通过 role 区分"对方说的"和"自己说的"。

### 3.2 CamelAgent（智能体）

核心方法 `step(inputMessage)`：
1. 将对方消息标记为 `user` 加入记忆
2. 调用 LLM（带 Inception Prompt 作为 system message）
3. 将回复标记为 `assistant` 加入记忆
4. 返回回复

**独立记忆设计**：每个 CamelAgent 有自己的 memory 列表，互不共享。这与 AutoGen（共享 chatHistory）形成鲜明对比。

### 3.3 InceptionPrompts（提示词模板）

提供三套提示词：
- `assistantInception()` → AI Assistant 的行为约束
- `userInception()` → AI User 的行为约束
- `taskSpecifier()` → 任务细化提示

### 3.4 RolePlaying（角色扮演协调器）

```
initChat() → 第一条指示
     ↓
step(inputMsg) → StepResult(assistantResponse, userResponse, terminated)
     ↓ (循环)
run(outputHandler) → 完整执行
```

## 4. 协作流程

```
[Task Specifier]
"写一本拖延症心理学电子书" → "按6章结构撰写，涵盖定义、成因、神经科学..."
        │
        ▼
[initChat: AI User 的第一条指示]
科普作家："指示：请撰写第一章..."
        │
        ▼
[第1轮: step()]
  ├── AI Assistant(心理学家) → "解决方案：拖延症是..."
  └── AI User(科普作家) → "指示：请撰写第二章..."
        │
        ▼
[第2轮: step()]
  ├── AI Assistant → "解决方案：拖延的心理成因..."
  └── AI User → "指示：请撰写第三章..."
        │
        ▼
  ... (更多轮次)
        │
        ▼
[终止]
科普作家："<CAMEL_TASK_DONE>"
```

## 5. 与 Python CAMEL 的差异

| 方面 | Python CAMEL | Java 实现 |
|------|-------------|----------|
| 依赖 | `camel-ai` pip 包 | Spring AI + Spring Boot |
| Agent 创建 | `RolePlaying` 自动创建 | 同样由 `RolePlaying` 构造器创建 |
| 模型调用 | `ModelBackend` + OpenAI API | `SpringAIChatClient` + Spring AI |
| 配置方式 | 代码中硬编码或 `.env` | `application.properties` + 环境变量 |
| 任务类型 | `TaskType` 枚举区分多种场景 | 简化为模板方法（仅实现 AI_SOCIETY） |
| Critic Agent | 可选的第三方评审 | 未实现（保持案例简洁） |

## 6. 为什么 CAMEL 是"轻架构、重提示"

对比三个已实现的框架：

- **AutoGen**：核心代码在 `RoundRobinGroupChat`（轮询调度、共享历史、终止检测）
- **AgentScope**：核心代码在 `MsgHub`（消息订阅/广播）和 `Pipeline`（流水线编排）
- **CAMEL**：核心代码在 `InceptionPrompts`（提示词模板），框架代码极简

CAMEL 的 `RolePlaying` 类不到 150 行，真正的"智能"完全依赖提示词工程。这是一种极具哲学意味的设计——与其构建复杂的协调基础设施，不如让 LLM 本身成为协调者。
