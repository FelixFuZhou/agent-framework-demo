package com.example.autogen.chat;

import com.example.autogen.message.ChatMessage;

/**
 * 文本匹配终止条件 - 对应AutoGen中的TextMentionTermination
 * 当消息内容包含指定关键词时，终止对话
 */
public class TextMentionTermination implements TerminationCondition {

    private final String keyword;

    public TextMentionTermination(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public boolean shouldTerminate(ChatMessage message) {
        return message.content() != null && message.content().contains(keyword);
    }
}
