package com.example.langgraph.node;

import com.example.langgraph.graph.GraphState;
import com.example.langgraph.graph.Node;
import com.example.langgraph.model.SpringAIChatClient;

/**
 * 回答节点 - 对应教程中的 generate_answer_node
 *
 * 工作流的第三步：基于搜索结果生成最终答案。
 *
 * 关键设计：根据上一步的搜索状态（成功/失败）采用不同策略：
 * - 搜索成功：基于搜索结果 + LLM 生成有据可依的回答
 * - 搜索失败：回退到 LLM 内部知识直接回答（降级策略）
 *
 * 这种条件逻辑体现了 LangGraph 状态驱动的设计优势：
 * 节点通过读取 state.step 来感知上游状态，实现弹性处理。
 */
public class AnswerNode implements Node {

    private final SpringAIChatClient chatClient;

    public AnswerNode(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public GraphState execute(GraphState state) {
        String userQuery = state.get(GraphState.USER_QUERY);
        String step = state.get(GraphState.STEP);

        String response;

        if ("search_failed".equals(step)) {
            // 搜索失败，回退策略
            String fallbackPrompt = """
                    搜索引擎暂时不可用，请基于你的知识回答用户的问题。
                    请在回答开头注明"（基于内部知识回答，未使用实时搜索）"。
                    
                    用户问题：%s""".formatted(userQuery);
            response = chatClient.chat(fallbackPrompt);
        } else {
            // 搜索成功，基于搜索结果生成答案
            String searchResults = state.get(GraphState.SEARCH_RESULTS);
            String answerPrompt = """
                    基于以下搜索结果为用户提供完整、准确的答案。
                    
                    用户问题：%s
                    
                    搜索结果：
                    %s
                    
                    请综合搜索结果，提供准确、有用的回答。要求：
                    1. 信息准确，有理有据
                    2. 结构清晰，分点阐述
                    3. 语言通俗易懂
                    4. 如有必要，提供实用建议""".formatted(userQuery, searchResults);
            response = chatClient.chat(answerPrompt);
        }

        return state
                .put(GraphState.FINAL_ANSWER, response)
                .put(GraphState.STEP, "completed")
                .appendMessage("[回答阶段] " + response);
    }
}
