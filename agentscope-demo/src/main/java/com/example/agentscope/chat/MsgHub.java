package com.example.agentscope.chat;

import com.example.agentscope.agent.Agent;
import com.example.agentscope.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 消息中心 - 对应 AgentScope 中的 MsgHub
 *
 * AgentScope 的核心创新：消息驱动架构。
 * MsgHub 负责消息的路由和分发，支持：
 * - 广播模式：消息自动分发给所有参与者
 * - 动态管理：运行时添加/移除参与者
 * - 消息持久化：记录所有消息历史
 *
 * 使用 try-with-resources 风格管理生命周期：
 * {@code
 * try (MsgHub hub = MsgHub.create(participants, announcement)) {
 *     hub.broadcast(msg);
 * }
 * }
 */
public class MsgHub implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MsgHub.class);

    private final List<Agent> participants;
    private final List<Msg> messageHistory = new ArrayList<>();
    private boolean autobroadcast;
    private Consumer<Msg> messageCallback;

    private MsgHub(List<Agent> participants, boolean autobroadcast) {
        this.participants = new ArrayList<>(participants);
        this.autobroadcast = autobroadcast;
    }

    /**
     * 创建 MsgHub 并发送公告消息
     *
     * @param participants  参与者列表
     * @param announcement  公告消息（会广播给所有参与者）
     * @return MsgHub 实例
     */
    public static MsgHub create(List<? extends Agent> participants, Msg announcement) {
        return create(participants, announcement, true);
    }

    public static MsgHub create(List<? extends Agent> participants, Msg announcement, boolean autobroadcast) {
        MsgHub hub = new MsgHub(List.copyOf(participants), autobroadcast);
        if (announcement != null) {
            hub.broadcast(announcement);
        }
        return hub;
    }

    /**
     * 广播消息给所有参与者（调用 observe）
     */
    public void broadcast(Msg message) {
        messageHistory.add(message);
        for (Agent agent : participants) {
            agent.observe(message);
        }
        if (messageCallback != null) {
            messageCallback.accept(message);
        }
    }

    /**
     * 让指定智能体发言，如果开启了自动广播，回复会自动分发给所有参与者
     *
     * @param agent   发言的智能体
     * @param message 传给智能体的消息
     * @return 智能体的回复
     */
    public Msg speak(Agent agent, Msg message) {
        Msg reply = agent.reply(message);
        if (reply != null && autobroadcast) {
            // 广播给其他参与者（发言者已在 reply 中处理了自己的记忆）
            messageHistory.add(reply);
            for (Agent other : participants) {
                if (other != agent) {
                    other.observe(reply);
                }
            }
            if (messageCallback != null) {
                messageCallback.accept(reply);
            }
        }
        return reply;
    }

    /**
     * 设置自动广播开关（例如投票阶段关闭广播，避免互相影响）
     */
    public void setAutobroadcast(boolean autobroadcast) {
        this.autobroadcast = autobroadcast;
    }

    /**
     * 动态添加参与者
     */
    public void add(Agent agent) {
        if (!participants.contains(agent)) {
            participants.add(agent);
        }
    }

    /**
     * 动态移除参与者
     */
    public void remove(Agent agent) {
        participants.remove(agent);
    }

    /**
     * 设置消息回调（用于外部监听消息流）
     */
    public void setMessageCallback(Consumer<Msg> callback) {
        this.messageCallback = callback;
    }

    /**
     * 获取消息历史
     */
    public List<Msg> getMessageHistory() {
        return List.copyOf(messageHistory);
    }

    public List<Agent> getParticipants() {
        return List.copyOf(participants);
    }

    @Override
    public void close() {
        log.debug("MsgHub 关闭，共 {} 条消息", messageHistory.size());
    }
}
