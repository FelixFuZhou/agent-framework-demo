package com.example.autogen.agent;

import com.example.autogen.message.ChatMessage;
import com.example.autogen.model.ModelClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 助理智能体 - 对应AutoGen中的AssistantAgent
 * 基于LLM的智能体，通过系统消息定义角色职责
 * 可以被赋予不同的"专家"角色（产品经理、工程师、代码审查员等）
 */
public class AssistantAgent implements Agent {

    private final String name;
    private final String systemMessage;
    private final ModelClient modelClient;

    public AssistantAgent(String name, String systemMessage, ModelClient modelClient) {
        this.name = name;
        this.systemMessage = systemMessage;
        this.modelClient = modelClient;
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
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统消息 - 定义角色
        messages.add(Map.of("role", "system", "content", systemMessage));

        // 将对话历史转换为OpenAI消息格式
        for (ChatMessage msg : chatHistory) {
            String role = msg.source().equals(name) ? "assistant" : "user";
            messages.add(Map.of(
                    "role", role,
                    "content", String.format("[%s]: %s", msg.source(), msg.content())
            ));
        }

        String response = modelClient.chatCompletion(messages);
        return new ChatMessage(name, response);
    }
}
