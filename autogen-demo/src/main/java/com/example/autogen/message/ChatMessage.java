package com.example.autogen.message;

import java.time.Instant;

/**
 * 消息对象 - AutoGen中智能体之间通信的基本单元
 */
public record ChatMessage(
        String source,   // 消息发送者名称
        String content,  // 消息内容
        Instant timestamp
) {
    public ChatMessage(String source, String content) {
        this(source, content, Instant.now());
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", source, content);
    }
}
