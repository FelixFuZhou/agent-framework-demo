package com.example.autogen.chat;

import com.example.autogen.message.ChatMessage;

/**
 * 终止条件接口 - 判断群聊何时结束
 */
@FunctionalInterface
public interface TerminationCondition {

    /**
     * 判断给定消息是否满足终止条件
     *
     * @param message 最新的消息
     * @return true 表示应该终止对话
     */
    boolean shouldTerminate(ChatMessage message);
}
