package com.example.agentscope.message;

import java.time.Instant;
import java.util.Map;

/**
 * 消息对象 - 对应 AgentScope 中的 Msg
 *
 * AgentScope 的核心创新在于消息驱动架构：所有智能体交互都被抽象为消息的发送和接收。
 *
 * @param name    发送者名称
 * @param content 消息内容
 * @param role    角色类型（user/assistant/system）
 * @param metadata 元数据（消息类型、优先级等）
 * @param timestamp 时间戳
 */
public record Msg(
        String name,
        String content,
        String role,
        Map<String, String> metadata,
        Instant timestamp
) {
    public Msg(String name, String content, String role) {
        this(name, content, role, Map.of(), Instant.now());
    }

    public Msg(String name, String content, String role, Map<String, String> metadata) {
        this(name, content, role, metadata, Instant.now());
    }

    @Override
    public String toString() {
        return String.format("[%s (%s)]: %s", name, role, content);
    }
}
