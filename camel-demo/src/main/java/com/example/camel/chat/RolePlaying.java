package com.example.camel.chat;

import com.example.camel.agent.CamelAgent;
import com.example.camel.message.ChatMessage;
import com.example.camel.model.SpringAIChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 角色扮演 - CAMEL 框架的核心协调机制
 *
 * RolePlaying 是 CAMEL 论文的核心贡献：通过 Inception Prompting 让两个 AI 智能体
 * 自主进入角色（AI User 和 AI Assistant），无需人类干预地完成复杂任务。
 *
 * 协作流程：
 * 1. initChat()  → AI User 发出第一条指示
 * 2. step()      → AI Assistant 生成解决方案 → AI User 评估并发出下一条指示
 * 3. 循环直到 AI User 输出 <CAMEL_TASK_DONE> 或达到最大轮次
 *
 * 与 AutoGen RoundRobinGroupChat 的核心区别：
 * - AutoGen：多智能体轮询，所有人共享同一对话历史
 * - CAMEL：双智能体深度协作，各自维护独立记忆，通过 Inception Prompt 实现自主驱动
 */
public class RolePlaying {

    private static final Logger log = LoggerFactory.getLogger(RolePlaying.class);
    private static final String TASK_DONE = "<CAMEL_TASK_DONE>";

    private final CamelAgent assistantAgent;
    private final CamelAgent userAgent;
    private final String specifiedTask;
    private final int maxTurns;

    /**
     * 单步执行结果
     *
     * @param assistantResponse AI Assistant 的回复（解决方案）
     * @param userResponse      AI User 的回复（下一条指示），终止时为 null
     * @param terminated        是否检测到终止信号
     */
    public record StepResult(
            ChatMessage assistantResponse,
            ChatMessage userResponse,
            boolean terminated
    ) {}

    /**
     * 创建角色扮演会话
     *
     * @param assistantRoleName AI Assistant 的角色名（如"心理学家"）
     * @param userRoleName      AI User 的角色名（如"科普作家"）
     * @param taskPrompt        任务描述
     * @param chatClient        模型客户端
     * @param maxTurns          最大对话轮次
     * @param specifyTask       是否使用 Task Specifier 细化任务
     */
    public RolePlaying(String assistantRoleName, String userRoleName,
                       String taskPrompt, SpringAIChatClient chatClient,
                       int maxTurns, boolean specifyTask) {
        this.maxTurns = maxTurns;

        // 可选：通过 Task Specifier 细化任务
        if (specifyTask) {
            String specifyPrompt = InceptionPrompts.taskSpecifier(taskPrompt, assistantRoleName, userRoleName);
            this.specifiedTask = chatClient.chat(specifyPrompt, List.of());
            System.out.println("【Task Specifier 细化后的任务】");
            System.out.println(specifiedTask);
            System.out.println();
        } else {
            this.specifiedTask = taskPrompt;
        }

        // 构建 Inception Prompts 并创建智能体
        String assistantSystemPrompt = InceptionPrompts.assistantInception(
                assistantRoleName, userRoleName, specifiedTask);
        String userSystemPrompt = InceptionPrompts.userInception(
                assistantRoleName, userRoleName, specifiedTask);

        this.assistantAgent = new CamelAgent(
                "AI-Assistant", assistantRoleName, assistantSystemPrompt, chatClient);
        this.userAgent = new CamelAgent(
                "AI-User", userRoleName, userSystemPrompt, chatClient);
    }

    /**
     * 初始化对话 - AI User 发出第一条指示
     *
     * 通过给 AI User 一条"启动消息"，让其根据 Inception Prompt 生成第一条指示。
     * 这是 CAMEL Role-Playing 的起点。
     */
    public ChatMessage initChat() {
        String openingMsg = "现在请开始指示我完成任务。记住：你必须按照格式 '指示：<你的指示>' 来给出指示。";
        return userAgent.step(new ChatMessage("assistant", openingMsg));
    }

    /**
     * 单步执行：一轮完整的 AI Assistant ↔ AI User 交互
     *
     * @param inputMsg AI User 发出的指示消息
     * @return 包含双方回复和终止状态的结果
     */
    public StepResult step(ChatMessage inputMsg) {
        // AI Assistant 根据指示生成解决方案
        ChatMessage assistantResponse = assistantAgent.step(inputMsg);

        // 检查 AI Assistant 是否认为任务完成
        if (assistantResponse.content().contains(TASK_DONE)) {
            return new StepResult(assistantResponse, null, true);
        }

        // AI User 评估解决方案并生成下一条指示
        ChatMessage userResponse = userAgent.step(assistantResponse);

        // 检查 AI User 是否认为任务完成
        boolean terminated = userResponse.content().contains(TASK_DONE);
        return new StepResult(assistantResponse, userResponse, terminated);
    }

    /**
     * 运行完整的角色扮演协作流程
     *
     * @param outputHandler 输出回调（每行输出都会调用）
     */
    public void run(Consumer<String> outputHandler) {
        outputHandler.accept("╔══════════════════════════════════════════╗");
        outputHandler.accept("║       CAMEL 角色扮演协作系统启动         ║");
        outputHandler.accept("╚══════════════════════════════════════════╝");
        outputHandler.accept("");
        outputHandler.accept("AI Assistant（执行者）: " + assistantAgent.getRoleName());
        outputHandler.accept("AI User（指挥者）: " + userAgent.getRoleName());
        outputHandler.accept("任务: " + specifiedTask);
        outputHandler.accept("");

        // 初始化：AI User 发出第一条指示
        ChatMessage inputMsg = initChat();
        outputHandler.accept("┌─────────────────────────────────────────┐");
        outputHandler.accept("│ " + userAgent.getRoleName() + " 的第一条指示：");
        outputHandler.accept("└─────────────────────────────────────────┘");
        outputHandler.accept(inputMsg.content());
        outputHandler.accept("");

        // 协作循环
        for (int turn = 1; turn <= maxTurns; turn++) {
            outputHandler.accept("========== 第 " + turn + " 轮对话 ==========");
            outputHandler.accept("");

            StepResult result = step(inputMsg);

            // 输出 AI Assistant 的解决方案
            outputHandler.accept("【" + assistantAgent.getRoleName() + " 的解决方案】");
            outputHandler.accept(result.assistantResponse().content());
            outputHandler.accept("");

            if (result.terminated()) {
                outputHandler.accept("╔══════════════════════════════════════════╗");
                outputHandler.accept("║         任务完成！协作结束               ║");
                outputHandler.accept("╚══════════════════════════════════════════╝");
                outputHandler.accept("共进行了 " + turn + " 轮对话。");
                return;
            }

            // 输出 AI User 的下一条指示
            outputHandler.accept("【" + userAgent.getRoleName() + " 的下一条指示】");
            outputHandler.accept(result.userResponse().content());
            outputHandler.accept("");

            inputMsg = result.userResponse();
        }

        outputHandler.accept("╔══════════════════════════════════════════╗");
        outputHandler.accept("║  已达到最大轮次（" + maxTurns + "），协作结束      ║");
        outputHandler.accept("╚══════════════════════════════════════════╝");
    }

    public String getSpecifiedTask() {
        return specifiedTask;
    }

    public CamelAgent getAssistantAgent() {
        return assistantAgent;
    }

    public CamelAgent getUserAgent() {
        return userAgent;
    }
}
