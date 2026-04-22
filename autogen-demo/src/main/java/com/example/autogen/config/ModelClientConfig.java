package com.example.autogen.config;

import com.example.autogen.model.ModelClient;
import com.example.autogen.model.OpenAIChatCompletionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型客户端配置 - 对应AutoGen中的create_openai_model_client()
 * 通过环境变量/配置文件管理API Key和服务地址
 */
@Configuration
public class ModelClientConfig {

    @Value("${llm.model:gpt-4o}")
    private String model;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Bean
    public ModelClient modelClient() {
        return new OpenAIChatCompletionClient(model, apiKey, baseUrl);
    }
}
