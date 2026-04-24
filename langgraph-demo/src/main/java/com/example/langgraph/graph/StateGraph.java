package com.example.langgraph.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 状态图 - 对应 LangGraph 中的 StateGraph
 *
 * LangGraph 的核心抽象：将智能体的执行流程建模为有向图。
 * - 节点（Node）：执行具体计算步骤
 * - 边（Edge）：定义节点之间的跳转逻辑
 * - 条件边（Conditional Edge）：根据状态动态路由
 *
 * 使用 Builder 模式构建图，调用 compile() 生成可执行的 CompiledGraph。
 *
 * 与其他框架的核心区别：
 * - AutoGen/CAMEL：对话驱动，行为从对话中涌现
 * - AgentScope：消息驱动，通过 MsgHub 路由
 * - LangGraph：图驱动，流程显式定义为状态机
 */
public class StateGraph {

    private static final Logger log = LoggerFactory.getLogger(StateGraph.class);

    public static final String END = "__end__";

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, String> edges = new LinkedHashMap<>();
    private final Map<String, ConditionalEdgeConfig> conditionalEdges = new LinkedHashMap<>();
    private String entryPoint;

    /**
     * 条件边配置：包含路由函数和路由映射表
     */
    record ConditionalEdgeConfig(
            ConditionalEdge routeFunction,
            Map<String, String> routeMap
    ) {}

    /**
     * 添加节点
     *
     * @param name 节点名称
     * @param node 节点执行函数
     */
    public StateGraph addNode(String name, Node node) {
        nodes.put(name, node);
        return this;
    }

    /**
     * 添加常规边（固定跳转）
     *
     * @param from 起始节点名
     * @param to   目标节点名（或 END）
     */
    public StateGraph addEdge(String from, String to) {
        edges.put(from, to);
        return this;
    }

    /**
     * 添加条件边（动态路由）
     *
     * @param from          起始节点名
     * @param routeFunction 路由判断函数
     * @param routeMap      路由映射：routeFunction 返回值 → 目标节点名
     */
    public StateGraph addConditionalEdges(String from, ConditionalEdge routeFunction,
                                          Map<String, String> routeMap) {
        conditionalEdges.put(from, new ConditionalEdgeConfig(routeFunction, routeMap));
        return this;
    }

    /**
     * 设置入口节点
     */
    public StateGraph setEntryPoint(String nodeName) {
        this.entryPoint = nodeName;
        return this;
    }

    /**
     * 编译图，生成可执行的应用
     *
     * @return 编译后的图
     */
    public CompiledGraph compile() {
        if (entryPoint == null) {
            throw new IllegalStateException("必须设置入口节点（setEntryPoint）");
        }
        if (!nodes.containsKey(entryPoint)) {
            throw new IllegalStateException("入口节点 '" + entryPoint + "' 未定义");
        }
        return new CompiledGraph(nodes, edges, conditionalEdges, entryPoint);
    }

    /**
     * 编译后的可执行图
     */
    public static class CompiledGraph {

        private final Map<String, Node> nodes;
        private final Map<String, String> edges;
        private final Map<String, ConditionalEdgeConfig> conditionalEdges;
        private final String entryPoint;

        CompiledGraph(Map<String, Node> nodes, Map<String, String> edges,
                      Map<String, ConditionalEdgeConfig> conditionalEdges, String entryPoint) {
            this.nodes = nodes;
            this.edges = edges;
            this.conditionalEdges = conditionalEdges;
            this.entryPoint = entryPoint;
        }

        /**
         * 执行图，流式输出每个节点的执行事件
         *
         * @param initialState  初始状态
         * @param eventHandler  事件回调（节点名 + 执行后状态）
         * @return 最终状态
         */
        public GraphState stream(GraphState initialState, Consumer<NodeEvent> eventHandler) {
            GraphState state = initialState;
            String currentNode = entryPoint;
            int maxSteps = 100; // 防止无限循环
            int step = 0;

            while (!END.equals(currentNode) && step < maxSteps) {
                step++;
                Node node = nodes.get(currentNode);
                if (node == null) {
                    throw new IllegalStateException("未知节点: " + currentNode);
                }

                log.debug("执行节点: {}", currentNode);
                state = node.execute(state);
                eventHandler.accept(new NodeEvent(currentNode, state));

                // 确定下一个节点
                currentNode = resolveNextNode(currentNode, state);
            }

            if (step >= maxSteps) {
                log.warn("图执行达到最大步数限制（{}），强制终止", maxSteps);
            }

            return state;
        }

        /**
         * 简单执行（不需要流式事件）
         */
        public GraphState invoke(GraphState initialState) {
            return stream(initialState, event -> {});
        }

        private String resolveNextNode(String currentNode, GraphState state) {
            // 优先检查条件边
            if (conditionalEdges.containsKey(currentNode)) {
                ConditionalEdgeConfig config = conditionalEdges.get(currentNode);
                String routeKey = config.routeFunction().route(state);
                String target = config.routeMap().get(routeKey);
                if (target == null) {
                    throw new IllegalStateException(
                            "条件边路由失败：节点 '%s' 返回了未知的路由键 '%s'"
                                    .formatted(currentNode, routeKey));
                }
                log.debug("条件路由: {} --[{}]--> {}", currentNode, routeKey, target);
                return target;
            }

            // 检查常规边
            if (edges.containsKey(currentNode)) {
                return edges.get(currentNode);
            }

            // 没有后续边，结束
            log.debug("节点 {} 没有后续边，图执行结束", currentNode);
            return END;
        }
    }

    /**
     * 节点执行事件 - 对应 LangGraph 中 stream() 输出的事件
     */
    public record NodeEvent(String nodeName, GraphState state) {}
}
