package com.example.autogen.agent;

import com.example.autogen.message.ChatMessage;

import java.util.List;

/**
 * Agent接口 - AutoGen中所有智能体的基础抽象
 * 每个智能体都有名称，能够根据对话历史生成回复
 */
public interface Agent {

    /**
     * 获取智能体名称
     */
    String getName();

    /**
     * 获取智能体描述
     */
    String getDescription();

    /**
     * 根据对话历史生成回复
     *
     * @param chatHistory 当前对话历史
     * @return 智能体的回复消息
     */
    ChatMessage reply(List<ChatMessage> chatHistory);
}
