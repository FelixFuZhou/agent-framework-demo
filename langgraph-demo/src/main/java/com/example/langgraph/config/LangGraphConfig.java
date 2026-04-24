package com.example.langgraph.config;

import com.example.langgraph.model.SpringAIChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangGraph 配置类
 */
@Configuration
public class LangGraphConfig {

    @Bean
    public SpringAIChatClient springAIChatClient(ChatClient.Builder chatClientBuilder) {
        return new SpringAIChatClient(chatClientBuilder.build());
    }
}
