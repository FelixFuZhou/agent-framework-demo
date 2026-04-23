package com.example.camel.message;

import java.time.Instant;

/**
 * 消息对象 - CAMEL 中智能体之间通信的基本单元
 *
 * CAMEL 的消息使用 role 标识发送方角色（user/assistant/system），
 * 而非 AutoGen 的 source 名称，因为 CAMEL 的核心是角色扮演：
 * AI User 和 AI Assistant 各自维护独立的对话历史。
 *
 * @param role      角色类型（user / assistant / system）
 * @param content   消息内容
 * @param timestamp 时间戳
 */
public record ChatMessage(
        String role,
        String content,
        Instant timestamp
) {
    public ChatMessage(String role, String content) {
        this(role, content, Instant.now());
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", role, content);
    }
}
