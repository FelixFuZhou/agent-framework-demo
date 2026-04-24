package com.example.langgraph.runner;

import com.example.langgraph.graph.GraphState;
import com.example.langgraph.graph.StateGraph;
import com.example.langgraph.model.SpringAIChatClient;
import com.example.langgraph.node.AnswerNode;
import com.example.langgraph.node.SearchNode;
import com.example.langgraph.node.UnderstandNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Scanner;

/**
 * 问答助手运行器 - LangGraph 三步问答助手案例入口
 *
 * 构建一个遵循"理解 → 搜索 → 回答"三步流程的问答助手。
 * 通过 StateGraph 将三个节点线性连接，并在 搜索→回答 之间
 * 加入条件边，实现搜索失败时的降级策略。
 *
 * 图结构：
 *   START → understand → search → [条件路由] → answer → END
 *                                  ↓ search_failed
 *                                  answer (降级模式)
 */
@Component
public class QAAssistantRunner implements CommandLineRunner {

    private final SpringAIChatClient chatClient;

    public QAAssistantRunner(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        // 构建状态图
        StateGraph.CompiledGraph app = buildGraph();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║    LangGraph 智能问答助手启动            ║");
        System.out.println("║    三步流程：理解 → 搜索 → 回答          ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();
        System.out.println("输入问题开始对话（输入 quit 退出）");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("🤔 您想了解什么: ");
            String input = scanner.nextLine().trim();

            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                System.out.println("再见！");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            System.out.println();
            System.out.println("════════════════════════════════════════════");

            // 创建初始状态
            GraphState initialState = new GraphState()
                    .put(GraphState.MESSAGES, input)
                    .put(GraphState.STEP, "start");

            // 流式执行图
            GraphState finalState = app.stream(initialState, event -> {
                String nodeName = event.nodeName();
                GraphState state = event.state();

                switch (nodeName) {
                    case "understand" -> {
                        System.out.println("🧠 理解阶段:");
                        System.out.println("   " + state.get(GraphState.USER_QUERY));
                        System.out.println("   搜索关键词: " + state.get(GraphState.SEARCH_QUERY));
                        System.out.println();
                    }
                    case "search" -> {
                        String step = state.get(GraphState.STEP);
                        if ("search_failed".equals(step)) {
                            System.out.println("❌ 搜索阶段: 搜索失败，将使用降级策略");
                        } else {
                            System.out.println("🔍 搜索阶段: 搜索完成");
                        }
                        System.out.println();
                    }
                    case "answer" -> {
                        System.out.println("💡 最终回答:");
                        System.out.println(state.get(GraphState.FINAL_ANSWER));
                    }
                }
            });

            System.out.println();
            System.out.println("════════════════════════════════════════════");
            System.out.println();
        }
    }

    /**
     * 构建三步问答助手的状态图
     *
     * 图结构：
     *   understand ---> search ---> answer ---> END
     *                      |
     *                      +-- (search_failed) --> answer (降级模式)
     *
     * 条件边在 search 和 answer 之间：
     *   - step == "searched" → 正常回答
     *   - step == "search_failed" → 降级回答
     * 两者都跳转到 answer 节点，但 answer 节点内部根据 step 状态选择不同策略
     */
    private StateGraph.CompiledGraph buildGraph() {
        StateGraph graph = new StateGraph();

        // 添加三个节点
        graph.addNode("understand", new UnderstandNode(chatClient));
        graph.addNode("search", new SearchNode(chatClient));
        graph.addNode("answer", new AnswerNode(chatClient));

        // 设置入口
        graph.setEntryPoint("understand");

        // 添加边：understand → search（常规边）
        graph.addEdge("understand", "search");

        // 添加条件边：search → 根据状态路由
        graph.addConditionalEdges("search",
                state -> {
                    String step = state.get(GraphState.STEP);
                    if ("search_failed".equals(step)) {
                        return "fallback";
                    }
                    return "success";
                },
                Map.of(
                        "success", "answer",
                        "fallback", "answer"
                )
        );

        // 添加边：answer → END
        graph.addEdge("answer", StateGraph.END);

        return graph.compile();
    }
}
