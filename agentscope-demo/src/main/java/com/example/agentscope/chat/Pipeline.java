package com.example.agentscope.chat;

import com.example.agentscope.agent.Agent;
import com.example.agentscope.message.Msg;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流管线 - 对应 AgentScope 中的 Pipeline
 *
 * 提供两种执行模式：
 * - 顺序管线（sequential_pipeline）：让智能体按顺序依次发言
 * - 扇出管线（fanout_pipeline）：同时向所有智能体发送消息并收集回复
 */
public final class Pipeline {

    private Pipeline() {}

    /**
     * 顺序管线 - 让智能体按列表顺序依次发言
     * 每个智能体的回复会作为下一个智能体的输入
     *
     * @param hub    消息中心
     * @param agents 智能体列表
     * @param initialMsg 初始消息
     * @return 最后一个智能体的回复
     */
    public static Msg sequential(MsgHub hub, List<? extends Agent> agents, Msg initialMsg) {
        Msg current = initialMsg;
        for (Agent agent : agents) {
            current = hub.speak(agent, current);
        }
        return current;
    }

    /**
     * 扇出管线 - 向所有智能体发送同一消息，收集各自回复
     * 模拟 AgentScope 中的 fanout_pipeline（同步版本）
     *
     * @param agents  智能体列表
     * @param message 发送给所有智能体的消息
     * @return 各智能体的回复列表
     */
    public static List<Msg> fanout(List<? extends Agent> agents, Msg message) {
        List<Msg> replies = new ArrayList<>();
        for (Agent agent : agents) {
            try {
                Msg reply = agent.reply(message);
                if (reply != null) {
                    replies.add(reply);
                }
            } catch (Exception e) {
                // 容错处理：某个智能体出错不影响其他
                replies.add(new Msg(agent.getName(), "（响应失败）", "assistant"));
            }
        }
        return replies;
    }
}
