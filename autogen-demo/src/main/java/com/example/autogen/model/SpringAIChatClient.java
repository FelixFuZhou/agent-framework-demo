package com.example.autogen.model;

import com.example.autogen.message.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型客户端 - 封装 Spring AI ChatClient 与 LLM 的交互
 * 对应AutoGen中的OpenAIChatCompletionClient
 *
 * 使用 Spring AI 提供的 ChatClient，支持 OpenAI 兼容接口（OpenAI / DeepSeek / 通义千问 / Ollama 等）
 */
public class SpringAIChatClient {

    private final ChatClient chatClient;

    public SpringAIChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 调用 LLM 生成回复
     *
     * @param systemMessage 系统消息（定义角色）
     * @param chatHistory   对话历史
     * @param agentName     当前智能体名称（用于区分 assistant/user 角色）
     * @return LLM 生成的回复文本
     */
    public String chat(String systemMessage, List<ChatMessage> chatHistory, String agentName) {
        List<Message> messages = new ArrayList<>();

        // 系统消息 - 定义角色
        messages.add(new SystemMessage(systemMessage));

        // 将对话历史转换为 Spring AI 消息格式
        for (ChatMessage msg : chatHistory) {
            String formattedContent = String.format("[%s]: %s", msg.source(), msg.content());
            if (msg.source().equals(agentName)) {
                messages.add(new AssistantMessage(formattedContent));
            } else {
                messages.add(new UserMessage(formattedContent));
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
