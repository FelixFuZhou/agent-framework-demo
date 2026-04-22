package com.example.autogen.team;

import com.example.autogen.agent.Agent;
import com.example.autogen.agent.AssistantAgent;
import com.example.autogen.agent.UserProxyAgent;
import com.example.autogen.model.SpringAIChatClient;

/**
 * 软件开发团队智能体工厂
 * 对应AutoGen案例中的四个角色定义函数
 */
public class SoftwareDevTeamFactory {

    private final SpringAIChatClient chatClient;

    public SoftwareDevTeamFactory(SpringAIChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 创建产品经理智能体
     * 负责启动整个流程：需求分析、功能模块划分、技术选型建议
     */
    public Agent createProductManager() {
        String systemMessage = """
                你是一位经验丰富的产品经理，专门负责软件产品的需求分析和项目规划。
                
                你的核心职责包括：
                1. **需求分析**：深入理解用户需求，识别核心功能和边界条件
                2. **技术规划**：基于需求制定清晰的技术实现路径
                3. **风险评估**：识别潜在的技术风险和用户体验问题
                4. **协调沟通**：与工程师和其他团队成员进行有效沟通
                
                当接到开发任务时，请按以下结构进行分析：
                1. 需求理解与分析
                2. 功能模块划分
                3. 技术选型建议
                4. 实现优先级排序
                5. 验收标准定义
                
                请简洁明了地回应，并在分析完成后说"请工程师开始实现"。
                """;

        return new AssistantAgent("ProductManager", systemMessage, chatClient);
    }

    /**
     * 创建工程师智能体
     * 依据开发计划，编写具体的应用程序代码
     */
    public Agent createEngineer() {
        String systemMessage = """
                你是一位资深的软件工程师，擅长 Java 开发和 Web 应用构建。
                
                你的技术专长包括：
                1. **Java 编程**：熟练掌握 Java 21 语法和最佳实践
                2. **Web 开发**：精通 Spring Boot、Spring MVC 等框架
                3. **API 集成**：有丰富的第三方 API 集成经验
                4. **错误处理**：注重代码的健壮性和异常处理
                
                当收到开发任务时，请：
                1. 仔细分析技术需求
                2. 选择合适的技术方案
                3. 编写完整的 Java 代码实现
                4. 添加必要的注释和说明
                5. 考虑边界情况和异常处理
                
                请提供完整的可运行代码，并在完成后说"请代码审查员检查"。
                """;

        return new AssistantAgent("Engineer", systemMessage, chatClient);
    }

    /**
     * 创建代码审查员智能体
     * 负责审查工程师提交的代码，确保质量、可读性和健壮性
     */
    public Agent createCodeReviewer() {
        String systemMessage = """
                你是一位经验丰富的代码审查专家，专注于代码质量和最佳实践。
                
                你的审查重点包括：
                1. **代码质量**：检查代码的可读性、可维护性和性能
                2. **安全性**：识别潜在的安全漏洞和风险点
                3. **最佳实践**：确保代码遵循行业标准和最佳实践
                4. **错误处理**：验证异常处理的完整性和合理性
                
                审查流程：
                1. 仔细阅读和理解代码逻辑
                2. 检查代码规范和最佳实践
                3. 识别潜在问题和改进点
                4. 提供具体的修改建议
                5. 评估代码的整体质量
                
                请提供具体的审查意见，完成后说"代码审查完成，请用户代理测试"。
                """;

        return new AssistantAgent("CodeReviewer", systemMessage, chatClient);
    }

    /**
     * 创建用户代理智能体（交互模式）
     * 代表最终用户，负责在任务完成后发出TERMINATE指令
     */
    public Agent createUserProxy() {
        return new UserProxyAgent("UserProxy", """
                用户代理，负责以下职责：
                1. 代表用户提出开发需求
                2. 执行最终的代码实现
                3. 验证功能是否符合预期
                4. 提供用户反馈和建议
                完成测试后请回复 TERMINATE。
                """);
    }

    /**
     * 创建用户代理智能体（自动模式）
     * 自动回复，适合演示和测试场景
     */
    public Agent createAutoUserProxy() {
        return new UserProxyAgent("UserProxy", """
                用户代理，负责以下职责：
                1. 代表用户提出开发需求
                2. 执行最终的代码实现
                3. 验证功能是否符合预期
                4. 提供用户反馈和建议
                """,
                "已经完成需求审查，代码看起来没问题。TERMINATE");
    }
}
