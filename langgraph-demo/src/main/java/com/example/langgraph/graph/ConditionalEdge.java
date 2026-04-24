package com.example.langgraph.graph;

/**
 * 条件路由函数 - 对应 LangGraph 中的条件边（Conditional Edges）
 *
 * 条件边是 LangGraph 最强大的特性之一：通过一个函数判断当前状态，
 * 动态决定下一步跳转到哪个节点。这是实现循环和分支的关键。
 */
@FunctionalInterface
public interface ConditionalEdge {

    /**
     * 根据当前状态决定路由目标
     *
     * @param state 当前全局状态
     * @return 路由键（映射到目标节点名），返回 "__end__" 表示结束
     */
    String route(GraphState state);
}
