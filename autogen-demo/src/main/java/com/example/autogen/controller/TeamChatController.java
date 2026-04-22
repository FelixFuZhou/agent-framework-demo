package com.example.autogen.controller;

import com.example.autogen.service.CodeExtractorService;
import com.example.autogen.service.CodeExtractorService.GeneratedFile;
import com.example.autogen.service.TeamChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 团队协作 Web 控制器
 * 提供 SSE 流式输出和用户交互的 REST API
 */
@RestController
@RequestMapping("/api/chat")
public class TeamChatController {

    private static final Logger log = LoggerFactory.getLogger(TeamChatController.class);

    private final TeamChatService teamChatService;
    private final CodeExtractorService codeExtractorService;

    public TeamChatController(TeamChatService teamChatService, CodeExtractorService codeExtractorService) {
        this.teamChatService = teamChatService;
        this.codeExtractorService = codeExtractorService;
    }

    /**
     * 启动团队协作，返回 SSE 事件流
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startChat(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("task 不能为空");
        }

        String sessionId = UUID.randomUUID().toString();
        log.info("启动团队协作会话: {}", sessionId);

        SseEmitter emitter = teamChatService.startChat(sessionId, task);

        // 先发送 sessionId 给前端
        try {
            emitter.send(SseEmitter.event()
                    .name("session")
                    .data(Map.of("sessionId", sessionId)));
        } catch (Exception e) {
            log.error("发送 sessionId 失败", e);
        }

        return emitter;
    }

    /**
     * 提交用户输入
     */
    @PostMapping("/input")
    public ResponseEntity<Map<String, Object>> submitInput(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String input = request.get("input");

        if (sessionId == null || input == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sessionId 和 input 不能为空"));
        }

        boolean success = teamChatService.submitUserInput(sessionId, input);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "会话不存在或已结束"));
        }
    }

    /**
     * 生成代码工程
     * 从对话历史中提取代码块，写入指定目录
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateCode(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String outputDir = request.get("outputDir");

        if (sessionId == null || outputDir == null || outputDir.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sessionId 和 outputDir 不能为空"));
        }

        try {
            var chatHistory = teamChatService.getChatHistory(sessionId);
            if (chatHistory.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "未找到对话历史"));
            }

            List<GeneratedFile> files = codeExtractorService.extractAndSave(chatHistory, outputDir);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "files", files,
                    "outputDir", outputDir
            ));
        } catch (Exception e) {
            log.error("代码生成失败", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
