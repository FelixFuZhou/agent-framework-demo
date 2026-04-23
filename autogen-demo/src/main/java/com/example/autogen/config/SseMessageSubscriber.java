package com.example.autogen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 订阅者 - 接收 "sse:*" channel 的消息，转发给 SseConnectionManager
 *
 * 直接实现 MessageListener，从 Message.getChannel() 获取真实 channel 名称。
 * 注意：不能通过 MessageListenerAdapter 包装，因为 Adapter 传给委托方法的
 * 第二个参数是订阅 pattern（"sse:*"），而非实际 channel（"sse:{sessionId}"）。
 */
@Component
public class SseMessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(SseMessageSubscriber.class);

    private final SseConnectionManager sseConnectionManager;

    public SseMessageSubscriber(SseConnectionManager sseConnectionManager) {
        this.sseConnectionManager = sseConnectionManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String payload = new String(message.getBody());

        if (!channel.startsWith("sse:")) {
            return;
        }
        String sessionId = channel.substring(4);
        log.debug("收到 Redis Pub/Sub 消息: channel={}, sessionId={}", channel, sessionId);
        sseConnectionManager.deliverToLocal(sessionId, payload);
    }
}
