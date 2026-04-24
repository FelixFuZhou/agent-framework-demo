package com.example.langgraph.graph;

/**
 * 节点接口 - 对应 LangGraph 中的 Node 函数
 *
 * 在 LangGraph 中，每个节点是一个接收当前状态、返回更新后状态的函数。
 * 节点是执行具体工作的单元（如调用 LLM、执行工具、处理数据）。
 */
@FunctionalInterface
public interface Node {

    /**
     * 执行节点逻辑
     *
     * @param state 当前全局状态
     * @return 更新后的状态
     */
    GraphState execute(GraphState state);
}
