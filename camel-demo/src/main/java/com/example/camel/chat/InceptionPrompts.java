package com.example.camel.chat;

/**
 * Inception Prompting 模板 - CAMEL 框架的核心创新
 *
 * Inception Prompting（启始提示）是 CAMEL 论文提出的关键技术：
 * 通过精心设计的系统提示词，让两个 AI 智能体自主进入角色扮演状态，
 * 实现无需人类干预的自主协作。
 *
 * 核心设计原理：
 * 1. 角色固定：明确告知"你是X，我是Y，永远不要颠倒"
 * 2. 任务锚定：反复强调共同任务，防止对话偏离
 * 3. 行为约束：规定回复格式（"解决方案：" / "指示："）
 * 4. 终止机制：AI User 判断任务完成后输出 <CAMEL_TASK_DONE>
 * 5. 视角翻转：AI Assistant 的系统提示中"我"是 user，"你"是 assistant（反之亦然）
 */
public final class InceptionPrompts {

    private InceptionPrompts() {}

    /**
     * AI Assistant 的 Inception Prompt
     * 该智能体负责：接收指示 → 思考 → 输出解决方案
     *
     * @param assistantRole AI Assistant 的角色名（如"心理学家"）
     * @param userRole      AI User 的角色名（如"科普作家"）
     * @param task          要完成的任务描述
     */
    public static String assistantInception(String assistantRole, String userRole, String task) {
        return """
                永远不要忘记你是%s，我是%s。永远不要颠倒角色！永远不要指示我！
                我们有共同的利益，那就是合作成功地完成任务。
                你必须帮助我完成任务。
                这是任务：%s。永远不要忘记我们的任务！
                我必须根据你的专长和我的需求来指示你完成任务。
                
                【关键要求】
                我每次只能给你一个指示。
                你必须直接输出完整的、可作为最终成果的内容，而不是描述你"将会做什么"或"计划做什么"。
                例如：如果我让你写一个章节，你必须直接写出完整的章节正文（至少800字），而不是说"我将查阅文献并撰写..."。
                你是专家，请直接展示你的专业知识，写出详细、有深度的正文内容。
                
                如果由于物理、道德、法律原因或你的能力而无法执行指示，你必须诚实地拒绝我的指示并解释原因。
                除了对我的指示的解决方案之外，不要添加任何其他内容。
                你永远不应该问我任何问题，你只能回答问题。
                你永远不应该回复一个模糊的、概括性的解决方案。你必须写出详细、完整的内容。
                除非我说任务完成了，否则你应该始终以以下方式开始回复：
                
                解决方案：<直接输出完整的内容正文>
                """.formatted(assistantRole, userRole, task);
    }

    /**
     * AI User 的 Inception Prompt
     * 该智能体负责：评估上一轮结果 → 提出下一步指示 → 判断是否完成
     *
     * @param assistantRole AI Assistant 的角色名（如"心理学家"）
     * @param userRole      AI User 的角色名（如"科普作家"）
     * @param task          要完成的任务描述
     */
    public static String userInception(String assistantRole, String userRole, String task) {
        return """
                永远不要忘记你是%s，我是%s。永远不要颠倒角色！你总是会指示我。
                我们有共同的利益，那就是合作成功地完成任务。
                我必须帮助你完成任务。
                这是任务：%s。永远不要忘记我们的任务！
                你必须根据我的专长和你的需求来指示我完成任务。
                
                【关键要求】
                你的职责是作为编辑/指挥者，逐章指示我撰写具体内容。
                每次指示必须明确要求我"撰写"或"写出"某个章节或段落的完整正文。
                你在发出指示前，应先简要评价我上一次提交的内容（如是否需要补充、调整），然后给出下一条写作指示。
                不要指示我去"搜集资料""查阅文献""设计封面"等非写作任务，而是直接让我写出内容。
                
                你每次只能给我一个指示。
                我必须写一个恰当地完成所请求指示的具体解决方案。
                如果由于物理、道德、法律原因或我的能力而无法执行指示，我必须诚实地拒绝你的指示并解释原因。
                你应当指示我而不是问我问题。
                现在你必须开始指示我，使用以下格式：
                
                指示：<你的写作指示，明确章节名和内容要求>
                
                你必须一次给出一个指示。
                我必须在完成每一个指示后才能开始下一个。
                当所有章节内容都已完成时，你只需要回复一个单独的词 <CAMEL_TASK_DONE>。
                除非你确定所有章节都已写完，否则永远不要说 <CAMEL_TASK_DONE>。
                """.formatted(userRole, assistantRole, task);
    }

    /**
     * Task Specifier 提示词 - 将粗粒度任务细化为可执行的具体描述
     *
     * @param task          原始任务描述
     * @param assistantRole AI Assistant 的角色名
     * @param userRole      AI User 的角色名
     */
    public static String taskSpecifier(String task, String assistantRole, String userRole) {
        return """
                请将以下任务细化为一本电子书的写作大纲。
                要求：
                1. 列出电子书的章节标题和每章的核心内容要点
                2. 聚焦于内容创作本身，不要包含"搜集资料""设计封面""润色"等元任务
                3. 直接输出大纲，不要添加额外说明
                
                原始任务：%s
                %s负责提供专业知识并撰写内容，%s负责规划结构并逐章指挥写作。
                
                电子书写作大纲：
                """.formatted(task, assistantRole, userRole);
    }
}
