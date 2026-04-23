package com.example.autogen.chat;

import com.example.autogen.agent.Agent;
import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.SpringAIChatClient;
import com.example.autogen.team.SoftwareDevTeamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 轮询群聊 - 对应AutoGen中的RoundRobinGroupChat
 *
 * 支持两种执行模式：
 * 1. run()          - 同步阻塞模式（CLI 场景）
 * 2. executeStep()  - 单步执行模式（事件驱动 Web 场景，状态存 Redis）
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

    // ==================== 同步阻塞模式（CLI） ====================

    /**
     * 运行群聊协作流程（同步方式，流式输出）
     *
     * @param task           初始任务描述
     * @param messageHandler 每产生一条消息的回调处理器（用于流式输出）
     * @return 完整的对话历史
     */
    public List<ChatMessage> run(String task, Consumer<ChatMessage> messageHandler) {
        List<ChatMessage> chatHistory = new ArrayList<>();

        ChatMessage taskMessage = new ChatMessage("user", task);
        chatHistory.add(taskMessage);
        messageHandler.accept(taskMessage);

        int turnCount = 0;
        int participantIndex = 0;

        while (turnCount < maxTurns) {
            Agent currentAgent = participants.get(participantIndex);
            log.info("---------- 轮次 {} | 智能体: {} ----------", turnCount + 1, currentAgent.getName());

            ChatMessage response = currentAgent.reply(chatHistory);
            chatHistory.add(response);
            messageHandler.accept(response);
            turnCount++;

            if (terminationCondition.shouldTerminate(response)) {
                log.info("检测到终止条件，对话结束。");
                break;
            }

            participantIndex = (participantIndex + 1) % participants.size();
        }

        if (turnCount >= maxTurns) {
            log.info("已达到最大轮次 ({})，对话结束。", maxTurns);
        }

        return chatHistory;
    }

    // ==================== 单步执行模式（事件驱动） ====================

    /**
     * 执行单步：让当前 Agent 发言，返回新状态和消息。
     * 如果当前是 UserProxy，返回 WAITING_INPUT 而不调 LLM。
     *
     * @param state   当前会话状态（从 Redis 读取）
     * @param factory 用于按名称创建 Agent 的工厂
     * @return 单步执行结果
     */
    public static StepResult executeStep(ChatSessionState state, SoftwareDevTeamFactory factory) {
        String currentAgentName = state.currentParticipantName();
        log.info("---------- 轮次 {} | 智能体: {} ----------", state.turnCount() + 1, currentAgentName);

        // UserProxy 不调 LLM，进入等待用户输入状态
        if ("UserProxy".equals(currentAgentName)) {
            return new StepResult(state.withStatus(ChatSessionState.Status.WAITING_INPUT), null);
        }

        // AI Agent 调用 LLM 生成回复
        Agent agent = factory.createByName(currentAgentName);
        ChatMessage response = agent.reply(state.chatHistory());

        // 更新状态
        ChatSessionState newState = state.appendMessageAndAdvance(response);

        // 判断终止
        if (state.shouldTerminate(response)) {
            log.info("检测到终止条件，对话结束。");
            newState = newState.withStatus(ChatSessionState.Status.COMPLETED);
        }

        return new StepResult(newState, response);
    }

    /**
     * 单步执行结果
     *
     * @param state   执行后的新状态
     * @param message 本步产生的消息（UserProxy 等待输入时为 null）
     */
    public record StepResult(ChatSessionState state, ChatMessage message) {}
}
