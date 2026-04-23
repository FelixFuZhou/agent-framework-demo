package com.example.autogen.controller;

import com.example.autogen.config.SseConnectionManager;
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
 * 团队协作 Web 控制器（事件驱动版）
 *
 * API 设计：
 * - POST /api/chat/start        → 创建会话，返回 sessionId（JSON）
 * - GET  /api/chat/stream/{id}  → SSE 事件流（支持断线重连）
 * - POST /api/chat/input        → 提交用户输入
 * - POST /api/chat/generate     → 从 Redis 读历史，生成代码
 */
@RestController
@RequestMapping("/api/chat")
public class TeamChatController {

    private static final Logger log = LoggerFactory.getLogger(TeamChatController.class);

    private final TeamChatService teamChatService;
    private final CodeExtractorService codeExtractorService;
    private final SseConnectionManager sseConnectionManager;

    public TeamChatController(TeamChatService teamChatService,
                              CodeExtractorService codeExtractorService,
                              SseConnectionManager sseConnectionManager) {
        this.teamChatService = teamChatService;
        this.codeExtractorService = codeExtractorService;
        this.sseConnectionManager = sseConnectionManager;
    }

    /**
     * 启动团队协作，返回 sessionId
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startChat(@RequestBody Map<String, String> request) {
        String task = request.get("task");
        if (task == null || task.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "task 不能为空"));
        }

        String sessionId = UUID.randomUUID().toString();
        log.info("启动团队协作会话: {}", sessionId);
        teamChatService.startChat(sessionId, task);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * SSE 事件流连接（支持断线重连）
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String sessionId) {
        log.info("SSE 连接: {}", sessionId);
        return sseConnectionManager.connect(sessionId);
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
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "会话不存在或未处于等待输入状态"));
        }
    }

    /**
     * 生成代码工程（从 Redis 读取对话历史）
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
