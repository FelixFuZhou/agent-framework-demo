package com.example.autogen.model;

import java.util.List;
import java.util.Map;

/**
 * 模型客户端接口 - 封装与LLM的交互
 * 对应AutoGen中的OpenAIChatCompletionClient
 */
public interface ModelClient {

    /**
     * 调用LLM生成回复
     *
     * @param messages 消息列表，格式为 [{role, content}]
     * @return LLM生成的回复文本
     */
    String chatCompletion(List<Map<String, String>> messages);
}
