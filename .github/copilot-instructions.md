# GitHub Copilot Instructions

## 项目概述

AI Agent 框架学习项目，用 Java 21 + Spring Boot 3.3.x 实现 AutoGen、AgentScope、CAMEL、LangGraph 四大框架的核心案例。

## 全局上下文

项目完整上下文见根目录 `AGENTS.md`。编码规范见 `docs/conventions/backend.md`。

## 编码要求

- Java 21：使用 record、text block、pattern matching、List.of/Map.of
- 按领域概念分包（agent/model/message/chat/config/runner/team）
- 接口不加 I 前缀，构造器注入，不用 @Autowired
- LLM 调用必须 try-catch
- 不可变数据载体使用 record
- 用户可见信息用 System.out.println，调试用 SLF4J
