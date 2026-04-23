package com.example.agentscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Demo 启动类
 *
 * 基于 AgentScope 框架的设计理念，用 Java 实现三国狼人杀游戏：
 * - 消息驱动架构：所有交互抽象为 Msg 的发送和接收
 * - MsgHub：消息中心，支持广播、点对点等通信模式
 * - DialogAgent：基于 LLM 的对话智能体
 * - 结构化输出：通过 JSON Schema 约束智能体行为
 *
 * 案例：三国狼人杀游戏（6 名角色：狼人×2、预言家×1、女巫×1、村民×2）
 */
@SpringBootApplication
public class AgentScopeDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeDemoApplication.class, args);
    }
}
