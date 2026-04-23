package com.example.autogen.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 基于 Redis 的会话状态仓储实现
 * key: chat:session:{sessionId}，TTL 2 小时
 */
@Repository
public class RedisChatSessionRepository implements ChatSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatSessionRepository.class);
    private static final String KEY_PREFIX = "chat:session:";
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatSessionRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ChatSessionState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(KEY_PREFIX + state.sessionId(), json, TTL);
        } catch (JsonProcessingException e) {
            log.error("序列化会话状态失败: {}", state.sessionId(), e);
            throw new RuntimeException("序列化会话状态失败", e);
        }
    }

    @Override
    public ChatSessionState findById(String sessionId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChatSessionState.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化会话状态失败: {}", sessionId, e);
            return null;
        }
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
