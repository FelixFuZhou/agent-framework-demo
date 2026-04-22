package com.example.autogen.service;

import com.example.autogen.agent.Agent;
import com.example.autogen.agent.WebUserProxyAgent;
import com.example.autogen.chat.RoundRobinGroupChat;
import com.example.autogen.chat.TextMentionTermination;
import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.SpringAIChatClient;
import com.example.autogen.team.SoftwareDevTeamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 团队聊天服务 - 管理 Web 模式下的多智能体协作会话
 */
@Service
public class TeamChatService {

    private static final Logger log = LoggerFactory.getLogger(TeamChatService.class);

    private final ChatClient.Builder chatClientBuilder;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /** 活跃会话：sessionId -> ChatSession */
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();

    /** 已完成会话的聊天历史（供代码生成使用） */
    private final Map<String, List<ChatMessage>> completedHistories = new ConcurrentHashMap<>();

    public TeamChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 启动团队协作会话，返回 SSE 发射器
     */
    public SseEmitter startChat(String sessionId, String task) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时

        SpringAIChatClient chatClient = new SpringAIChatClient(chatClientBuilder.build());
        SoftwareDevTeamFactory factory = new SoftwareDevTeamFactory(chatClient);

        WebUserProxyAgent webUserProxy = new WebUserProxyAgent("UserProxy", "用户代理");

        List<Agent> participants = List.of(
                factory.createProductManager(),
                factory.createEngineer(),
                factory.createCodeReviewer(),
                webUserProxy
        );

        RoundRobinGroupChat teamChat = new RoundRobinGroupChat(
                participants,
                new TextMentionTermination("TERMINATE"),
                20
        );

        ChatSession session = new ChatSession(emitter, webUserProxy, teamChat);
        sessions.put(sessionId, session);

        // 当 UserProxy 等待输入时，向前端发送通知
        webUserProxy.setOnWaitingForInput(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("waiting_input")
                        .data(Map.of("message", "请输入您的回复")));
            } catch (IOException e) {
                log.error("通知等待输入失败", e);
            }
        });

        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        emitter.onError(e -> sessions.remove(sessionId));

        executor.submit(() -> {
            try {
                List<ChatMessage> result = teamChat.run(task, msg -> {
                    try {
                        Map<String, String> data = Map.of(
                                "source", msg.source(),
                                "content", msg.content(),
                                "timestamp", msg.timestamp().toString()
                        );
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(data));

                        // 当轮到 UserProxy 且是等待输入状态，通知前端
                        if (msg.source().equals("UserProxy")) {
                            // UserProxy 的消息已发出，不需要额外通知
                        }
                    } catch (IOException e) {
                        log.error("SSE 发送失败", e);
                    }
                });

                // 聊天结束，发送完成事件
                session.setChatHistory(result);
                completedHistories.put(sessionId, result);
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of("totalTurns", result.size())));
                emitter.complete();
            } catch (Exception e) {
                log.error("团队协作异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", e.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                sessions.remove(sessionId);
            }
        });

        return emitter;
    }

    /**
     * 提交用户输入到指定会话
     */
    public boolean submitUserInput(String sessionId, String input) {
        ChatSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        session.webUserProxy().submitInput(input);
        return true;
    }

    /**
     * 获取会话的聊天历史（用于代码提取）
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        // 优先从已完成的历史中获取
        List<ChatMessage> completed = completedHistories.get(sessionId);
        if (completed != null) {
            return completed;
        }
        ChatSession session = sessions.get(sessionId);
        return session != null ? session.chatHistory() : List.of();
    }

    /**
     * 会话数据
     */
    private static class ChatSession {
        private final SseEmitter emitter;
        private final WebUserProxyAgent webUserProxy;
        private final RoundRobinGroupChat teamChat;
        private List<ChatMessage> chatHistory = List.of();

        ChatSession(SseEmitter emitter, WebUserProxyAgent webUserProxy, RoundRobinGroupChat teamChat) {
            this.emitter = emitter;
            this.webUserProxy = webUserProxy;
            this.teamChat = teamChat;
        }

        SseEmitter emitter() { return emitter; }
        WebUserProxyAgent webUserProxy() { return webUserProxy; }
        List<ChatMessage> chatHistory() { return chatHistory; }
        void setChatHistory(List<ChatMessage> chatHistory) { this.chatHistory = chatHistory; }
    }
}
