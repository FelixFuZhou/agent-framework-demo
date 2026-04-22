package com.example.autogen.runner;

import com.example.autogen.agent.Agent;
import com.example.autogen.chat.RoundRobinGroupChat;
import com.example.autogen.chat.TextMentionTermination;
import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.SpringAIChatClient;
import com.example.autogen.team.SoftwareDevTeamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 软件开发团队协作运行器
 * 对应AutoGen案例中的run_software_development_team()
 *
 * 基于 Spring AI ChatClient 实现 AutoGen 的多智能体对话协作：
 * ProductManager(需求分析) -> Engineer(编码) -> CodeReviewer(审查) -> UserProxy(验收)
 */
@Component
public class SoftwareDevTeamRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SoftwareDevTeamRunner.class);

    private final ChatClient.Builder chatClientBuilder;

    public SoftwareDevTeamRunner(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public void run(String... args) {
        System.out.println("🔧 正在初始化 Spring AI ChatClient...");
        System.out.println("👥 正在创建智能体团队...");

        // 构建 Spring AI ChatClient
        SpringAIChatClient chatClient = new SpringAIChatClient(chatClientBuilder.build());

        // 创建团队工厂
        SoftwareDevTeamFactory factory = new SoftwareDevTeamFactory(chatClient);

        // 创建四个智能体角色
        Agent productManager = factory.createProductManager();
        Agent engineer = factory.createEngineer();
        Agent codeReviewer = factory.createCodeReviewer();
        Agent userProxy = factory.createAutoUserProxy(); // 使用自动模式演示

        System.out.println("🚀 启动 AutoGen 软件开发团队协作...");
        System.out.println("=".repeat(60));

        // 定义团队聊天和协作规则 - 对应AutoGen的RoundRobinGroupChat
        RoundRobinGroupChat teamChat = new RoundRobinGroupChat(
                List.of(productManager, engineer, codeReviewer, userProxy),
                new TextMentionTermination("TERMINATE"),
                20  // 最大轮次（安全阀）
        );

        // 定义任务描述
        String task = """
                我们需要开发一个比特币价格显示应用，具体要求如下：
                核心功能：
                - 实时显示比特币当前价格（USD）
                - 显示24小时价格变化趋势（涨跌幅和涨跌额）
                - 提供价格刷新功能
                
                技术要求：
                - 使用 Java + Spring Boot 框架创建 Web 应用
                - 界面简洁美观，用户友好
                - 添加适当的错误处理和加载状态
                
                请团队协作完成这个任务，从需求分析到最终实现。
                """;

        // 运行团队协作，流式输出对话过程
        List<ChatMessage> result = teamChat.run(task, this::printMessage);

        System.out.println("=".repeat(60));
        System.out.println("✅ 团队协作完成！");
        System.out.println();
        System.out.printf("📋 协作结果摘要：%n");
        System.out.printf("- 参与智能体数量：4个%n");
        System.out.printf("- 总对话轮次：%d轮%n", result.size());
        System.out.printf("- 任务完成状态：成功%n");
    }

    private void printMessage(ChatMessage message) {
        String separator = "-".repeat(10);
        String sourceType = message.source().equals("user") ? "TextMessage (user)" : "TextMessage (" + message.source() + ")";
        System.out.printf("%s %s %s%n", separator, sourceType, separator);
        System.out.println(message.content());
        System.out.println();
    }
}
