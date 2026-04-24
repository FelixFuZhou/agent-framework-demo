package com.example.langgraph.graph;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局状态 - 对应 LangGraph 中的 TypedDict State
 *
 * LangGraph 的核心设计：整个图的执行过程都围绕一个共享的状态对象进行。
 * 所有节点都能读取和更新这个中心状态，状态在节点间传递形成数据流。
 *
 * 本实现使用 Map<String, String> 作为底层存储，提供类型安全的访问方法。
 */
public class GraphState {

    public static final String MESSAGES = "messages";
    public static final String USER_QUERY = "user_query";
    public static final String SEARCH_QUERY = "search_query";
    public static final String SEARCH_RESULTS = "search_results";
    public static final String FINAL_ANSWER = "final_answer";
    public static final String STEP = "step";

    private final Map<String, String> data;

    public GraphState() {
        this.data = new HashMap<>();
    }

    public GraphState(Map<String, String> data) {
        this.data = new HashMap<>(data);
    }

    public String get(String key) {
        return data.get(key);
    }

    public GraphState put(String key, String value) {
        data.put(key, value);
        return this;
    }

    /**
     * 追加消息到消息历史（用换行分隔）
     */
    public GraphState appendMessage(String message) {
        String existing = data.getOrDefault(MESSAGES, "");
        String updated = existing.isEmpty() ? message : existing + "\n" + message;
        data.put(MESSAGES, updated);
        return this;
    }

    /**
     * 创建当前状态的副本（用于不可变更新模式）
     */
    public GraphState copy() {
        return new GraphState(this.data);
    }

    public Map<String, String> toMap() {
        return Map.copyOf(data);
    }

    @Override
    public String toString() {
        return "GraphState" + data;
    }
}
