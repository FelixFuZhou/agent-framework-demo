package com.example.autogen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoGen Demo 启动类
 *
 * 演示AutoGen框架的核心机制：
 * - AssistantAgent: 基于LLM的智能体，通过SystemMessage定义角色
 * - UserProxyAgent: 用户代理，负责发起任务和验收
 * - RoundRobinGroupChat: 轮询群聊，按预定义顺序协调多智能体发言
 * - TextMentionTermination: 终止条件，检测TERMINATE关键词结束对话
 *
 * 案例：软件开发团队协作
 * ProductManager -> Engineer -> CodeReviewer -> UserProxy
 */
@SpringBootApplication
public class AutoGenDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoGenDemoApplication.class, args);
    }
}
