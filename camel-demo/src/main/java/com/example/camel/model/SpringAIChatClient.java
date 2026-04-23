package com.example.camel.model;

import com.example.camel.message.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型客户端 - 封装 Spring AI ChatClient 与 LLM 的交互
 *
 * CAMEL 的模型调用与 AutoGen/AgentScope 的区别在于：
 * 每个 CamelAgent 维护独立的对话历史（memory），消息角色直接使用 user/assistant，
 * 不需要通过 agentName 判断角色映射。
 */
public class SpringAIChatClient {

    private final ChatClient chatClient;

    public SpringAIChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 调用 LLM 生成回复
     *
     * @param systemPrompt 系统提示词（Inception Prompt）
     * @param history      对话历史
     * @return LLM 生成的回复文本
     */
    public String chat(String systemPrompt, List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for (ChatMessage msg : history) {
            switch (msg.role()) {
                case "user" -> messages.add(new UserMessage(msg.content()));
                case "assistant" -> messages.add(new AssistantMessage(msg.content()));
                default -> messages.add(new SystemMessage(msg.content()));
            }
        }

        try {
            return chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();
        } catch (Exception e) {
            return "（模型调用失败：" + e.getMessage() + "）";
        }
    }
}
