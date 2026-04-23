package com.example.autogen.chat;

/**
 * 会话状态仓储接口
 */
public interface ChatSessionRepository {

    void save(ChatSessionState state);

    ChatSessionState findById(String sessionId);

    void delete(String sessionId);
}
