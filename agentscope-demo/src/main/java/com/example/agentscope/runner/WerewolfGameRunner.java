package com.example.agentscope.runner;

import com.example.agentscope.agent.DialogAgent;
import com.example.agentscope.game.Player;
import com.example.agentscope.game.RolePrompts;
import com.example.agentscope.game.ThreeKingdomsWerewolfGame;
import com.example.agentscope.model.SpringAIChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 三国狼人杀游戏运行器
 *
 * 演示 AgentScope 框架的核心机制：
 * - 消息驱动：所有交互通过 Msg 传递
 * - MsgHub：狼人专属频道、白天全体讨论
 * - Pipeline：顺序讨论（sequential）、并行投票（fanout）
 * - DialogAgent：基于 LLM 的角色扮演智能体
 * - 结构化输出：JSON 格式约束游戏行为
 */
@Component
public class WerewolfGameRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WerewolfGameRunner.class);

    private final ChatClient.Builder chatClientBuilder;

    public WerewolfGameRunner(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public void run(String... args) {
        System.out.println("🔧 正在初始化 Spring AI ChatClient...");
        System.out.println("👥 正在创建三国狼人杀游戏角色...");
        System.out.println();

        SpringAIChatClient chatClient = new SpringAIChatClient(chatClientBuilder.build());

        // 创建游戏主持人（不通过 LLM，只用于广播消息）
        DialogAgent moderator = new DialogAgent("游戏主持人", "你是狼人杀游戏的主持人。", chatClient);

        // 创建六名玩家（三国人物 × 狼人杀角色）
        List<Player> players = RolePrompts.createDefaultPlayers(chatClient);

        System.out.println("🚀 启动三国狼人杀游戏...");
        System.out.println("=".repeat(60));
        System.out.println();

        // 启动游戏
        ThreeKingdomsWerewolfGame game = new ThreeKingdomsWerewolfGame(
                players, moderator, System.out::println);
        game.run();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("✅ 三国狼人杀游戏结束！");
    }
}
