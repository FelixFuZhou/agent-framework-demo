package com.example.autogen.chat;

import com.example.autogen.message.ChatMessage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 可序列化的会话状态 - 存储在 Redis 中
 * 包含 RoundRobinGroupChat 的所有运行时变量，任何节点都可以恢复执行。
 */
public record ChatSessionState(
        String sessionId,
        Status status,
        List<ChatMessage> chatHistory,
        int turnCount,
        int participantIndex,
        int maxTurns,
        String terminationKeyword,
        List<String> participantNames,
        Instant createdAt,
        Instant updatedAt
) {

    public enum Status {
        RUNNING,
        WAITING_INPUT,
        COMPLETED,
        ERROR
    }

    @JsonCreator
    public ChatSessionState(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("status") Status status,
            @JsonProperty("chatHistory") List<ChatMessage> chatHistory,
            @JsonProperty("turnCount") int turnCount,
            @JsonProperty("participantIndex") int participantIndex,
            @JsonProperty("maxTurns") int maxTurns,
            @JsonProperty("terminationKeyword") String terminationKeyword,
            @JsonProperty("participantNames") List<String> participantNames,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt
    ) {
        this.sessionId = sessionId;
        this.status = status;
        this.chatHistory = List.copyOf(chatHistory);
        this.turnCount = turnCount;
        this.participantIndex = participantIndex;
        this.maxTurns = maxTurns;
        this.terminationKeyword = terminationKeyword;
        this.participantNames = List.copyOf(participantNames);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建初始会话状态
     */
    public static ChatSessionState initial(String sessionId, String task,
                                           List<String> participantNames,
                                           String terminationKeyword, int maxTurns) {
        ChatMessage taskMessage = new ChatMessage("user", task);
        return new ChatSessionState(
                sessionId, Status.RUNNING,
                List.of(taskMessage),
                0, 0, maxTurns, terminationKeyword,
                participantNames,
                Instant.now(), Instant.now()
        );
    }

    /**
     * 追加消息并推进到下一个参与者
     */
    public ChatSessionState appendMessageAndAdvance(ChatMessage message) {
        List<ChatMessage> newHistory = new ArrayList<>(chatHistory);
        newHistory.add(message);
        int newIndex = (participantIndex + 1) % participantNames.size();
        return new ChatSessionState(
                sessionId, status, newHistory,
                turnCount + 1, newIndex, maxTurns,
                terminationKeyword, participantNames,
                createdAt, Instant.now()
        );
    }

    /**
     * 仅追加消息（用户输入，不增加 turnCount）
     */
    public ChatSessionState appendUserInput(ChatMessage message) {
        List<ChatMessage> newHistory = new ArrayList<>(chatHistory);
        newHistory.add(message);
        int newIndex = (participantIndex + 1) % participantNames.size();
        return new ChatSessionState(
                sessionId, Status.RUNNING, newHistory,
                turnCount + 1, newIndex, maxTurns,
                terminationKeyword, participantNames,
                createdAt, Instant.now()
        );
    }

    public ChatSessionState withStatus(Status newStatus) {
        return new ChatSessionState(
                sessionId, newStatus, chatHistory,
                turnCount, participantIndex, maxTurns,
                terminationKeyword, participantNames,
                createdAt, Instant.now()
        );
    }

    /**
     * 获取当前该发言的参与者名称
     */
    public String currentParticipantName() {
        return participantNames.get(participantIndex);
    }

    /**
     * 检查是否应该终止
     */
    public boolean shouldTerminate(ChatMessage message) {
        if (message.content() != null && message.content().contains(terminationKeyword)) {
            return true;
        }
        return turnCount + 1 >= maxTurns;
    }
}
