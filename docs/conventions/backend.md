# Java 后端编码规范

> 本规范适用于项目中所有 Java 模块，确保 AI 生成和人工编写的代码风格一致。

## 1. 技术基线

- Java 21（使用 record、sealed、text block、pattern matching 等新特性）
- Spring Boot 3.3.x
- Maven 构建

## 2. 分层架构

本项目各模块按职责分包，**不做 controller/service/dao 分层**（非 Web 业务应用），而是按领域概念分包：

```
com.example.{module}/
├── {Module}Application.java    # Spring Boot 启动类
├── agent/                      # 智能体定义（Agent 接口及实现）
├── model/                      # 模型客户端（与 LLM 交互）
├── message/                    # 消息定义
├── chat/                       # 群聊/协调机制
├── config/                     # Spring 配置类
├── runner/                     # CommandLineRunner 启动入口
└── team/                       # 团队/工厂（组合智能体的高层逻辑）
```

## 3. 命名约定

### 类命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 接口 | 名词/动名词，不加 I 前缀 | `Agent`, `ModelClient`, `TerminationCondition` |
| 实现类 | 描述性名称 | `AssistantAgent`, `OpenAIChatCompletionClient` |
| 配置类 | `*Config` | `ModelClientConfig` |
| 启动器 | `*Runner` | `SoftwareDevTeamRunner` |
| 工厂类 | `*Factory` | `SoftwareDevTeamFactory` |
| 数据载体 | 使用 `record` | `ChatMessage` |

### 方法命名

- 创建对象：`create*()` — `createProductManager()`
- 判断布尔：`should*()` / `is*()` — `shouldTerminate()`
- 核心行为：动词开头 — `reply()`, `run()`, `chatCompletion()`

## 4. Java 21 特性使用规范

### 优先使用 record 作为不可变数据载体

```java
// ✅ 推荐
public record ChatMessage(String source, String content, Instant timestamp) {
    public ChatMessage(String source, String content) {
        this(source, content, Instant.now());
    }
}

// ❌ 避免：为简单数据对象写冗长的 class + getter/setter
```

### 使用 Text Block 编写多行字符串

```java
// ✅ 推荐：System Message 等长文本用 text block
String systemMessage = """
        你是一位经验丰富的产品经理，专门负责软件产品的需求分析和项目规划。
        你的核心职责包括：
        1. 需求分析
        2. 技术规划
        """;

// ❌ 避免：字符串拼接
```

### 使用不可变集合

```java
// ✅ 推荐
List.of(agent1, agent2, agent3)
Map.of("role", "system", "content", message)

// ❌ 避免：需要可变时才用 new ArrayList<>()
```

## 5. Spring 相关约定

### 配置注入

```java
// ✅ 使用 @Value + 环境变量回退
@Value("${llm.model:gpt-4o}")
private String model;

// ✅ 构造器注入（不用 @Autowired）
public SoftwareDevTeamRunner(ModelClient modelClient) {
    this.modelClient = modelClient;
}
```

### Bean 定义

- 配置类使用 `@Configuration` + `@Bean`
- 启动逻辑实现 `CommandLineRunner`
- 不使用 `@Component` 扫描业务核心类（如 Agent），由工厂显式创建

## 6. LLM 交互规范

### 请求构造

```java
// 消息列表格式统一为 List<Map<String, String>>
List<Map<String, String>> messages = new ArrayList<>();
messages.add(Map.of("role", "system", "content", systemMessage));
messages.add(Map.of("role", "user", "content", userMessage));
```

### 错误处理

- LLM 调用必须 try-catch，返回可读的错误提示而非抛出异常
- 日志记录使用 SLF4J：`log.error("调用LLM失败: {}", e.getMessage(), e)`

## 7. 日志规范

- 使用 SLF4J + Logback（Spring Boot 默认）
- 控制台输出用 `System.out.println` 用于用户可见的交互信息
- 调试/错误信息用 `log.info/error`
- 不使用 `System.err`

## 8. 依赖管理

- 公共依赖版本在父 POM 的 `<dependencyManagement>` 中统一管理
- 各子模块只声明依赖，不指定版本号
- Spring Boot 管理的依赖不重复声明版本
