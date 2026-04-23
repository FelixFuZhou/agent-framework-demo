package com.example.agentscope.agent;

import com.example.agentscope.message.Msg;

/**
 * 智能体基础接口 - 对应 AgentScope 中的 AgentBase
 *
 * AgentScope 中每个智能体都有明确的生命周期，基于统一的基类实现。
 * 开发者通常只需关注 reply() 和 observe() 两个核心方法。
 */
public interface Agent {

    /**
     * 获取智能体名称
     */
    String getName();

    /**
     * 核心响应逻辑 - 智能体收到消息后"思考并回应"
     *
     * @param message 收到的消息
     * @return 智能体的回复消息
     */
    Msg reply(Msg message);

    /**
     * 观察逻辑 - 智能体被动接收消息（加入记忆，但不产生回复）
     *
     * @param message 观察到的消息
     */
    void observe(Msg message);
}
