package com.example.autogen.chat;

import com.example.autogen.agent.Agent;
import com.example.autogen.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 轮询群聊 - 对应AutoGen中的RoundRobinGroupChat
 *
 * 核心协调机制：让参与的智能体按照预定义的顺序依次发言。
 * 适用于流程固定的任务，如软件开发流程：
 * 产品经理 -> 工程师 -> 代码审查员 -> 用户代理
 *
 * 工作流：
 * 1. 将所有参与协作的智能体加入参与者列表
 * 2. 任务开始时，按预设顺序依次激活智能体
 * 3. 被选中的智能体根据当前对话上下文进行响应
 * 4. 将新回复加入对话历史，激活下一个智能体
 * 5. 持续进行，直到达到最大轮次或满足终止条件
 */
public class RoundRobinGroupChat {

    private static final Logger log = LoggerFactory.getLogger(RoundRobinGroupChat.class);

    private final List<Agent> participants;
    private final TerminationCondition terminationCondition;
    private final int maxTurns;

    public RoundRobinGroupChat(List<Agent> participants,
                               TerminationCondition terminationCondition,
                               int maxTurns) {
        this.participants = List.copyOf(participants);
        this.terminationCondition = terminationCondition;
        this.maxTurns = maxTurns;
    }

    /**
     * 运行群聊协作流程（同步方式，流式输出）
     *
     * @param task           初始任务描述
     * @param messageHandler 每产生一条消息的回调处理器（用于流式输出）
     * @return 完整的对话历史
     */
    public List<ChatMessage> run(String task, Consumer<ChatMessage> messageHandler) {
        List<ChatMessage> chatHistory = new ArrayList<>();

        // 将初始任务作为用户消息加入对话历史
        ChatMessage taskMessage = new ChatMessage("user", task);
        chatHistory.add(taskMessage);
        messageHandler.accept(taskMessage);

        int turnCount = 0;
        int participantIndex = 0;

        while (turnCount < maxTurns) {
            // 按轮询顺序选择下一个智能体
            Agent currentAgent = participants.get(participantIndex);

            log.info("---------- 轮次 {} | 智能体: {} ----------", turnCount + 1, currentAgent.getName());

            // 智能体根据对话历史生成回复
            ChatMessage response = currentAgent.reply(chatHistory);
            chatHistory.add(response);
            messageHandler.accept(response);

            turnCount++;

            // 检查终止条件
            if (terminationCondition.shouldTerminate(response)) {
                log.info("检测到终止条件，对话结束。");
                break;
            }

            // 轮询到下一个智能体
            participantIndex = (participantIndex + 1) % participants.size();
        }

        if (turnCount >= maxTurns) {
            log.info("已达到最大轮次 ({})，对话结束。", maxTurns);
        }

        return chatHistory;
    }
}
