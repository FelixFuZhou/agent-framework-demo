package com.example.autogen.agent;

import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.SpringAIChatClient;

import java.util.List;

/**
 * 助理智能体 - 对应AutoGen中的AssistantAgent
 * 基于 Spring AI ChatClient 的智能体，通过系统消息定义角色职责
 * 可以被赋予不同的"专家"角色（产品经理、工程师、代码审查员等）
 */
public class AssistantAgent implements Agent {

    private final String name;
    private final String systemMessage;
    private final SpringAIChatClient chatClient;

    public AssistantAgent(String name, String systemMessage, SpringAIChatClient chatClient) {
        this.name = name;
        this.systemMessage = systemMessage;
        this.chatClient = chatClient;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "AssistantAgent: " + name;
    }

    @Override
    public ChatMessage reply(List<ChatMessage> chatHistory) {
        String response = chatClient.chat(systemMessage, chatHistory, name);
        return new ChatMessage(name, response);
    }
}
