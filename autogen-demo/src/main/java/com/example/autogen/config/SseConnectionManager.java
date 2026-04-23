package com.example.autogen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 * 管理本机的 SSE 连接，通过 Redis Pub/Sub 接收其他节点转发的事件。
 *
 * 多节点流程：
 * 1. 前端 GET /stream/{sessionId} → 本机注册 SseEmitter
 * 2. 任何节点产生消息 → 发布到 Redis channel "sse:{sessionId}"
 * 3. 所有节点收到 Pub/Sub → 检查本机是否持有该 sessionId 的 SseEmitter → 有则推送
 */
@Component
public class SseConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionManager.class);

    private final Map<String, SseEmitter> localEmitters = new ConcurrentHashMap<>();
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SseConnectionManager(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 前端建立 SSE 连接时注册
     */
    public SseEmitter connect(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        localEmitters.put(sessionId, emitter);
        emitter.onCompletion(() -> localEmitters.remove(sessionId));
        emitter.onTimeout(() -> localEmitters.remove(sessionId));
        emitter.onError(e -> localEmitters.remove(sessionId));

        // 发送初始事件，立即刷新 HTTP 响应头到客户端
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.error("发送初始 SSE 事件失败: sessionId={}", sessionId, e);
        }

        return emitter;
    }

    /**
     * 发送 SSE 事件（通过 Redis Pub/Sub 广播到所有节点）
     */
    public void send(String sessionId, String eventName, Object data) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "event", eventName,
                    "data", data
            ));
            redisTemplate.convertAndSend("sse:" + sessionId, payload);
        } catch (Exception e) {
            log.error("发布 SSE 事件到 Redis 失败: sessionId={}, event={}", sessionId, eventName, e);
        }
    }

    /**
     * 收到 Redis Pub/Sub 消息后，推送给本机持有的 SSE 连接
     * 由 SseMessageSubscriber 回调
     */
    @SuppressWarnings("unchecked")
    public void deliverToLocal(String sessionId, String payload) {
        SseEmitter emitter = localEmitters.get(sessionId);
        if (emitter == null) {
            return; // 本机没有这个 session 的 SSE 连接
        }

        try {
            Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
            String eventName = (String) parsed.get("event");
            Object data = parsed.get("data");

            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));

            // complete 事件后关闭连接
            if ("complete".equals(eventName) || "error".equals(eventName)) {
                emitter.complete();
                localEmitters.remove(sessionId);
            }
        } catch (IOException e) {
            log.error("SSE 本地推送失败: sessionId={}", sessionId, e);
            localEmitters.remove(sessionId);
        }
    }
}
