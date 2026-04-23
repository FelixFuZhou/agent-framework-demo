package com.example.camel.config;

import com.example.camel.model.SpringAIChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CAMEL 配置类 - 创建模型客户端 Bean
 */
@Configuration
public class CamelConfig {

    @Bean
    public SpringAIChatClient springAIChatClient(ChatClient.Builder chatClientBuilder) {
        return new SpringAIChatClient(chatClientBuilder.build());
    }
}
