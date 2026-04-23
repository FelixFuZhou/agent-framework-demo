package com.example.autogen.service;

import com.example.autogen.chat.ChatSessionRepository;
import com.example.autogen.chat.ChatSessionState;
import com.example.autogen.chat.ChatSessionState.Status;
import com.example.autogen.chat.RoundRobinGroupChat;
import com.example.autogen.chat.RoundRobinGroupChat.StepResult;
import com.example.autogen.config.SseConnectionManager;
import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.SpringAIChatClient;
import com.example.autogen.team.SoftwareDevTeamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 团队聊天服务 - 事件驱动模式
 *
 * 核心思想：把 while 循环拆成单步执行。
 * 每一步做完后状态写回 Redis，遇到 UserProxy 就暂停等待用户输入，
 * 收到用户输入后从 Redis 恢复状态继续。
 *
 * 任何节点都可以处理 /start 和 /input 请求，无需 sticky session。
 */
@Service
public class TeamChatService {

    private static final Logger log = LoggerFactory.getLogger(TeamChatService.class);
    private static final List<String> PARTICIPANT_NAMES =
            List.of("ProductManager", "Engineer", "CodeReviewer", "UserProxy");

    private final ChatClient.Builder chatClientBuilder;
    private final ChatSessionRepository sessionRepository;
    private final SseConnectionManager sseManager;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TeamChatService(ChatClient.Builder chatClientBuilder,
                           ChatSessionRepository sessionRepository,
                           SseConnectionManager sseManager) {
        this.chatClientBuilder = chatClientBuilder;
        this.sessionRepository = sessionRepository;
        this.sseManager = sseManager;
    }

    /**
     * 创建会话并启动协作
     */
    public String startChat(String sessionId, String task) {
        ChatSessionState state = ChatSessionState.initial(
                sessionId, task, PARTICIPANT_NAMES, "TERMINATE", 20);
        sessionRepository.save(state);

        // 推送初始任务消息
        sseManager.send(sessionId, "message", Map.of(
                "source", "user",
                "content", task,
                "timestamp", state.chatHistory().getFirst().timestamp().toString()
        ));

        // 异步驱动
        driveSession(sessionId);
        return sessionId;
    }

    /**
     * 接收用户输入，更新状态，继续驱动
     */
    public boolean submitUserInput(String sessionId, String input) {
        ChatSessionState state = sessionRepository.findById(sessionId);
        if (state == null || state.status() != Status.WAITING_INPUT) {
            return false;
        }

        ChatMessage userMessage = new ChatMessage("UserProxy", input);
        ChatSessionState newState = state.appendUserInput(userMessage);

        // 检查是否终止
        if (newState.shouldTerminate(userMessage)) {
            newState = newState.withStatus(Status.COMPLETED);
        }

        sessionRepository.save(newState);

        // 推送用户消息
        sseManager.send(sessionId, "message", Map.of(
                "source", userMessage.source(),
                "content", userMessage.content(),
                "timestamp", userMessage.timestamp().toString()
        ));

        if (newState.status() == Status.COMPLETED) {
            sseManager.send(sessionId, "complete", Map.of("totalTurns", newState.turnCount()));
        } else {
            driveSession(sessionId);
        }

        return true;
    }

    /**
     * 获取对话历史（从 Redis）
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        ChatSessionState state = sessionRepository.findById(sessionId);
        return state != null ? state.chatHistory() : List.of();
    }

    /**
     * 核心驱动方法：从 Redis 读取状态，循环执行直到 WAITING_INPUT 或 COMPLETED
     */
    private void driveSession(String sessionId) {
        executor.submit(() -> {
            try {
                // 每次 drive 创建新的 ChatClient（无状态）
                SpringAIChatClient chatClient = new SpringAIChatClient(chatClientBuilder.build());
                SoftwareDevTeamFactory factory = new SoftwareDevTeamFactory(chatClient);

                while (true) {
                    ChatSessionState state = sessionRepository.findById(sessionId);
                    if (state == null) {
                        log.warn("会话不存在: {}", sessionId);
                        break;
                    }
                    if (state.status() == Status.COMPLETED || state.status() == Status.ERROR) {
                        break;
                    }
                    if (state.status() == Status.WAITING_INPUT) {
                        sseManager.send(sessionId, "waiting_input", Map.of("message", "请输入您的回复"));
                        break;
                    }

                    // 执行单步
                    StepResult result = RoundRobinGroupChat.executeStep(state, factory);
                    sessionRepository.save(result.state());

                    // 推送消息
                    if (result.message() != null) {
                        sseManager.send(sessionId, "message", Map.of(
                                "source", result.message().source(),
                                "content", result.message().content(),
                                "timestamp", result.message().timestamp().toString()
                        ));
                    }

                    // 检查新状态
                    if (result.state().status() == Status.COMPLETED) {
                        sseManager.send(sessionId, "complete",
                                Map.of("totalTurns", result.state().turnCount()));
                        break;
                    }
                    if (result.state().status() == Status.WAITING_INPUT) {
                        sseManager.send(sessionId, "waiting_input",
                                Map.of("message", "请输入您的回复"));
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("驱动会话异常: {}", sessionId, e);
                sseManager.send(sessionId, "error", Map.of("message", e.getMessage()));
                ChatSessionState state = sessionRepository.findById(sessionId);
                if (state != null) {
                    sessionRepository.save(state.withStatus(Status.ERROR));
                }
            }
        });
    }
}
