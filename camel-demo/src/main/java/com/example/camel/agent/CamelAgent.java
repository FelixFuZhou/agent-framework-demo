package com.example.camel.agent;

import com.example.camel.message.ChatMessage;
import com.example.camel.model.SpringAIChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * CAMEL 智能体实现 - 基于 Inception Prompt 的角色扮演智能体
 *
 * 每个 CamelAgent 对应 CAMEL 中的一个角色（AI User 或 AI Assistant），
 * 通过 Inception Prompt 定义其行为模式：
 * - AI Assistant（如心理学家）：接收指示，生成解决方案
 * - AI User（如科普作家）：发出指示，驱动任务推进
 *
 * 核心设计：每个智能体维护独立的 memory，对方的消息作为 user 角色，
 * 自己的回复作为 assistant 角色，实现"视角翻转"。
 */
public class CamelAgent implements ChatAgent {

    private static final Logger log = LoggerFactory.getLogger(CamelAgent.class);

    private final String name;
    private final String roleName;
    private final String systemPrompt;
    private final SpringAIChatClient chatClient;
    private final List<ChatMessage> memory = new ArrayList<>();

    public CamelAgent(String name, String roleName, String systemPrompt, SpringAIChatClient chatClient) {
        this.name = name;
        this.roleName = roleName;
        this.systemPrompt = systemPrompt;
        this.chatClient = chatClient;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRoleName() {
        return roleName;
    }

    /**
     * 单步对话：接收对方消息 → 加入记忆 → 调用 LLM → 返回回复
     *
     * 输入消息被标记为 "user" 角色（对方的话），
     * LLM 的回复被标记为 "assistant" 角色（自己的话）。
     * 这样每个智能体都"认为"自己是 assistant，对方是 user。
     */
    @Override
    public ChatMessage step(ChatMessage inputMessage) {
        memory.add(new ChatMessage("user", inputMessage.content()));

        log.debug("{}（{}）正在思考...", name, roleName);
        String response = chatClient.chat(systemPrompt, memory);

        ChatMessage reply = new ChatMessage("assistant", response);
        memory.add(reply);
        return reply;
    }

    @Override
    public void reset() {
        memory.clear();
    }
}
