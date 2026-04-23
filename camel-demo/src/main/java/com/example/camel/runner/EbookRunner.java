package com.example.camel.runner;

import com.example.camel.chat.RolePlaying;
import com.example.camel.model.SpringAIChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 电子书生成运行器 - CAMEL 角色扮演案例入口
 *
 * 案例说明：
 * 模拟 CAMEL 论文中的经典场景 —— 两个 AI 智能体通过角色扮演自主协作。
 * - AI Assistant（心理学家）：提供专业的心理学知识和内容
 * - AI User（科普作家）：规划电子书结构，逐步指示心理学家撰写各章节
 *
 * 协作产出：一本关于"拖延症心理学"的科普电子书内容
 */
@Component
public class EbookRunner implements CommandLineRunner {

    private final SpringAIChatClient chatClient;

    public EbookRunner(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        String task = """
                写一本关于拖延症心理学的科普电子书，内容包括：
                1. 拖延症的定义与常见误解
                2. 拖延的心理成因（完美主义、恐惧失败、即时满足偏好等）
                3. 拖延症的神经科学基础
                4. 拖延对个人生活和工作的影响
                5. 科学验证的克服拖延方法（如番茄工作法、实施意图、自我同情等）
                6. 总结与行动指南""";

        var rolePlaying = new RolePlaying(
                "心理学家",       // AI Assistant - 提供专业知识
                "科普作家",       // AI User - 规划结构并发出指示
                task,
                chatClient,
                10,               // 最大对话轮次
                true              // 启用 Task Specifier 细化任务
        );

        rolePlaying.run(System.out::println);
    }
}
