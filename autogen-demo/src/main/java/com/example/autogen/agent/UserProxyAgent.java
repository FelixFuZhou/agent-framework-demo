package com.example.autogen.agent;

import com.example.autogen.message.ChatMessage;

import java.util.List;
import java.util.Scanner;

/**
 * 用户代理智能体 - 对应AutoGen中的UserProxyAgent
 *
 * 不依赖LLM进行回复，而是作为用户在系统中的代理。
 * 扮演双重角色：
 * 1. 人类用户的"代言人"，负责发起任务和传达意图
 * 2. 可靠的"执行器"，负责在任务最终完成后发出TERMINATE指令
 *
 * 支持两种模式：
 * - 交互模式：等待用户输入
 * - 自动模式：根据预设的回复自动响应
 */
public class UserProxyAgent implements Agent {

    private final String name;
    private final String description;
    private final boolean interactive;
    private final String autoReply;

    /**
     * 交互模式构造器 - 等待用户手动输入
     */
    public UserProxyAgent(String name, String description) {
        this.name = name;
        this.description = description;
        this.interactive = true;
        this.autoReply = null;
    }

    /**
     * 自动模式构造器 - 使用预设回复
     */
    public UserProxyAgent(String name, String description, String autoReply) {
        this.name = name;
        this.description = description;
        this.interactive = false;
        this.autoReply = autoReply;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ChatMessage reply(List<ChatMessage> chatHistory) {
        if (interactive) {
            System.out.print("请输入您的回复 (输入 TERMINATE 结束): ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            return new ChatMessage(name, input);
        } else {
            return new ChatMessage(name, autoReply);
        }
    }
}
