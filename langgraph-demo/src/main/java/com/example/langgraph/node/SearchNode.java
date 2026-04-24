package com.example.langgraph.node;

import com.example.langgraph.graph.GraphState;
import com.example.langgraph.graph.Node;
import com.example.langgraph.model.SpringAIChatClient;

/**
 * 搜索节点 - 对应教程中的 tavily_search_node
 *
 * 工作流的第二步：执行信息搜索。
 *
 * 原版教程使用 Tavily API 进行真实的互联网搜索。
 * 本实现使用 LLM 模拟搜索过程（因为不依赖外部搜索 API），
 * 但保留了 LangGraph 的核心设计模式：
 * 1. 节点接收状态、更新状态
 * 2. 包含错误处理（搜索失败时标记 step 为 search_failed）
 * 3. 状态中的 step 字段驱动后续节点的条件分支
 */
public class SearchNode implements Node {

    private final SpringAIChatClient chatClient;

    public SearchNode(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public GraphState execute(GraphState state) {
        String searchQuery = state.get(GraphState.SEARCH_QUERY);

        try {
            String prompt = """
                    你是一个知识搜索引擎。请针对以下搜索查询，提供5条最相关的知识要点。
                    每条要点需要简洁、准确、有信息量。
                    
                    搜索查询：%s
                    
                    请以编号列表格式输出搜索结果：""".formatted(searchQuery);

            String results = chatClient.chat(prompt);

            return state
                    .put(GraphState.SEARCH_RESULTS, results)
                    .put(GraphState.STEP, "searched")
                    .appendMessage("[搜索阶段] 搜索完成，找到相关信息");

        } catch (Exception e) {
            return state
                    .put(GraphState.SEARCH_RESULTS, "搜索失败：" + e.getMessage())
                    .put(GraphState.STEP, "search_failed")
                    .appendMessage("[搜索阶段] 搜索遇到问题：" + e.getMessage());
        }
    }
}
