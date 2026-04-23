package com.example.agentscope.agent;

import com.example.agentscope.message.Msg;
import com.example.agentscope.model.SpringAIChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话智能体 - 对应 AgentScope 中的 DialogAgent
 *
 * 基于 LLM 的对话智能体，通过系统提示词定义角色和行为。
 * 内置记忆管理：维护对话历史，用于生成上下文感知的回复。
 */
public class DialogAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DialogAgent.class);

    private final String name;
    private final String systemPrompt;
    private final SpringAIChatClient chatClient;
    private final List<Msg> memory = new ArrayList<>();

    public DialogAgent(String name, String systemPrompt, SpringAIChatClient chatClient) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.chatClient = chatClient;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Msg reply(Msg message) {
        // 将收到的消息加入记忆
        if (message != null) {
            memory.add(message);
        }

        // 调用 LLM 生成回复
        String response = chatClient.chat(systemPrompt, memory, name);
        Msg reply = new Msg(name, response, "assistant");

        // 将自己的回复也加入记忆
        memory.add(reply);
        return reply;
    }

    /**
     * 带结构化输出约束的回复 - 通过 JSON Schema 提示约束输出格式
     *
     * @param message      收到的消息
     * @param formatPrompt 格式化提示（要求 LLM 以特定 JSON 格式输出）
     * @return 智能体的回复消息
     */
    public Msg reply(Msg message, String formatPrompt) {
        if (message != null) {
            memory.add(message);
        }

        // 将格式约束追加到系统提示中
        String enhancedPrompt = systemPrompt + "\n\n" + formatPrompt;
        String response = chatClient.chat(enhancedPrompt, memory, name);
        Msg reply = new Msg(name, response, "assistant");
        memory.add(reply);
        return reply;
    }

    @Override
    public void observe(Msg message) {
        if (message != null) {
            memory.add(message);
        }
    }

    /**
     * 清空记忆
     */
    public void clearMemory() {
        memory.clear();
    }

    /**
     * 获取记忆列表（只读视图）
     */
    public List<Msg> getMemory() {
        return List.copyOf(memory);
    }
}
