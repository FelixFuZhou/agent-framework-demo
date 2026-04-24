package com.example.langgraph.model;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;

/**
 * 模型客户端 - 封装 Spring AI ChatClient 与 LLM 的交互
 *
 * LangGraph 中每个节点可以独立调用 LLM，模型客户端作为共享的基础设施。
 */
public class SpringAIChatClient {

    private final ChatClient chatClient;

    public SpringAIChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 使用系统提示词调用 LLM
     */
    public String chat(String systemPrompt) {
        try {
            return chatClient.prompt()
                    .messages(List.<Message>of(new SystemMessage(systemPrompt)))
                    .call()
                    .content();
        } catch (Exception e) {
            return "（模型调用失败：" + e.getMessage() + "）";
        }
    }
}
