package com.example.autogen.agent;

import com.example.autogen.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Web 用户代理智能体
 * 通过 BlockingQueue 接收来自 Web 客户端的用户输入，替代 Scanner 的 stdin 交互。
 */
public class WebUserProxyAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(WebUserProxyAgent.class);

    private final String name;
    private final String description;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private Runnable onWaitingForInput;

    public WebUserProxyAgent(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 设置等待输入时的回调（用于通知 Web 前端）
     */
    public void setOnWaitingForInput(Runnable onWaitingForInput) {
        this.onWaitingForInput = onWaitingForInput;
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
        try {
            if (onWaitingForInput != null) {
                onWaitingForInput.run();
            }
            log.info("等待用户 Web 输入...");
            String input = inputQueue.poll(10, TimeUnit.MINUTES);
            if (input == null) {
                return new ChatMessage(name, "TERMINATE");
            }
            return new ChatMessage(name, input);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ChatMessage(name, "TERMINATE");
        }
    }

    /**
     * 从 Web 端提交用户输入
     */
    public void submitInput(String input) {
        inputQueue.offer(input);
    }
}
