package com.example.agentscope.model;

import com.example.agentscope.message.Msg;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型客户端 - 封装 Spring AI ChatClient 与 LLM 的交互
 * 对应 AgentScope 中的 Model API 层
 */
public class SpringAIChatClient {

    private final ChatClient chatClient;

    public SpringAIChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 调用 LLM 生成回复
     *
     * @param systemPrompt 系统提示词
     * @param memory       对话历史（Msg 列表）
     * @param agentName    当前智能体名称
     * @return LLM 生成的回复文本
     */
    public String chat(String systemPrompt, List<Msg> memory, String agentName) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for (Msg msg : memory) {
            String formattedContent = String.format("[%s]: %s", msg.name(), msg.content());
            if (msg.name().equals(agentName)) {
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
