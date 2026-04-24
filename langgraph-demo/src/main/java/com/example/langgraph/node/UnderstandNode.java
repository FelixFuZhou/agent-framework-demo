package com.example.langgraph.node;

import com.example.langgraph.graph.GraphState;
import com.example.langgraph.graph.Node;
import com.example.langgraph.model.SpringAIChatClient;

/**
 * 理解节点 - 对应教程中的 understand_query_node
 *
 * 工作流的第一步：分析用户查询意图，生成优化后的搜索关键词。
 * 体现了 LangGraph 的设计理念：每个节点是一个执行具体任务的独立函数。
 */
public class UnderstandNode implements Node {

    private final SpringAIChatClient chatClient;

    public UnderstandNode(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public GraphState execute(GraphState state) {
        String userMessage = state.get(GraphState.MESSAGES);

        String prompt = """
                分析用户的查询："%s"
                请完成两个任务：
                1. 简洁总结用户想要了解什么
                2. 生成最适合搜索引擎的关键词（中英文均可，要精准）
                
                格式：
                理解：[用户需求总结]
                搜索词：[最佳搜索关键词]""".formatted(userMessage);

        String response = chatClient.chat(prompt);

        // 解析搜索关键词
        String searchQuery = userMessage;
        if (response.contains("搜索词：")) {
            searchQuery = response.split("搜索词：")[1].trim();
            // 取第一行
            if (searchQuery.contains("\n")) {
                searchQuery = searchQuery.split("\n")[0].trim();
            }
        }

        return state
                .put(GraphState.USER_QUERY, response)
                .put(GraphState.SEARCH_QUERY, searchQuery)
                .put(GraphState.STEP, "understood")
                .appendMessage("[理解阶段] " + response);
    }
}
