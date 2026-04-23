package com.example.camel.agent;

import com.example.camel.message.ChatMessage;

/**
 * 智能体接口 - 对应 CAMEL 中的 ChatAgent
 *
 * CAMEL 的智能体设计特点：
 * 1. 每个智能体维护独立的对话记忆（memory）
 * 2. 通过 step() 方法实现单步对话：接收一条输入消息，返回一条回复
 * 3. 智能体的角色和行为完全由 Inception Prompt（系统提示词）定义
 */
public interface ChatAgent {

    /**
     * 获取智能体名称
     */
    String getName();

    /**
     * 获取角色名称（如"心理学家"、"科普作家"）
     */
    String getRoleName();

    /**
     * 单步对话 - CAMEL 的核心交互方式
     *
     * @param inputMessage 输入消息（来自对方智能体）
     * @return 智能体的回复消息
     */
    ChatMessage step(ChatMessage inputMessage);

    /**
     * 重置智能体的对话记忆
     */
    void reset();
}
