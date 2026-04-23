package com.example.agentscope.game;

import com.example.agentscope.agent.DialogAgent;
import com.example.agentscope.model.SpringAIChatClient;

import java.util.List;

/**
 * 角色提示词工厂 - 融合游戏规则与三国人物性格
 *
 * 为每个智能体注入"游戏角色"和"三国人格"的双重身份提示词。
 */
public final class RolePrompts {

    private RolePrompts() {}

    /**
     * 生成角色提示词 - 融合游戏规则与人物性格
     */
    public static String getRolePrompt(GameRole role, String character) {
        String basePrompt = """
                你是%s，在这场三国狼人杀游戏中扮演%s。
                
                重要规则：
                1. 你只能通过对话和推理参与游戏
                2. 严格按照要求的格式回复
                3. 回复要简洁，每次不超过100字
                4. 以%s的性格和语气说话
                """.formatted(character, role.displayName(), character);

        return basePrompt + switch (role) {
            case WEREWOLF -> """
                    
                    角色特点：
                    - 你是狼人阵营，目标是消灭所有好人
                    - 夜晚可以与其他狼人协商击杀目标
                    - 白天要隐藏身份，误导好人
                    - 以%s的性格说话和行动
                    """.formatted(character);
            case SEER -> """
                    
                    角色特点：
                    - 你是预言家，属于好人阵营
                    - 夜晚可以查验一名玩家的真实身份（狼人/好人）
                    - 白天可以选择性地公开查验结果来引导投票
                    - 注意保护自己，不要过早暴露身份
                    """;
            case WITCH -> """
                    
                    角色特点：
                    - 你是女巫，属于好人阵营
                    - 拥有一瓶解药（可救人）和一瓶毒药（可毒人）
                    - 每种药只能使用一次
                    - 不能同时在同一夜使用两种药
                    - 要谨慎使用药物，关键时刻才出手
                    """;
            case VILLAGER -> """
                    
                    角色特点：
                    - 你是村民，属于好人阵营
                    - 没有特殊能力，但可以通过观察和推理识别狼人
                    - 白天讨论时积极发言，分析可疑行为
                    - 投票是你最重要的武器
                    """;
        };
    }

    /**
     * 创建讨论阶段的格式化提示（用于结构化输出）
     */
    public static String getDiscussionFormat() {
        return """
                请分析当前局势并表达你的观点。回复时请严格使用以下JSON格式：
                {"analysis": "你的分析", "suspect": "你怀疑的玩家姓名或null", "confidence": 1到10的整数}
                只输出JSON，不要输出其他内容。
                """;
    }

    /**
     * 创建投票阶段的格式化提示
     */
    public static String getVoteFormat(List<String> candidates) {
        return """
                现在进入投票阶段，请从以下存活玩家中选择一人投票淘汰：
                %s
                请严格使用以下JSON格式回复：
                {"target": "你投票的玩家姓名"}
                只输出JSON，不要输出其他内容。
                """.formatted(String.join("、", candidates));
    }

    /**
     * 创建狼人击杀阶段的格式化提示
     */
    public static String getWerewolfKillFormat(List<String> candidates) {
        return """
                狼人阶段，请从以下存活的好人玩家中选择一人击杀：
                %s
                请严格使用以下JSON格式回复：
                {"target": "你要击杀的玩家姓名"}
                只输出JSON，不要输出其他内容。
                """.formatted(String.join("、", candidates));
    }

    /**
     * 创建预言家查验阶段的格式化提示
     */
    public static String getSeerCheckFormat(List<String> candidates) {
        return """
                预言家阶段，请从以下存活玩家中选择一人查验身份：
                %s
                请严格使用以下JSON格式回复：
                {"target": "你要查验的玩家姓名"}
                只输出JSON，不要输出其他内容。
                """.formatted(String.join("、", candidates));
    }

    /**
     * 创建女巫行动阶段的格式化提示
     */
    public static String getWitchActionFormat(String victim, boolean hasAntidote, boolean hasPoison, List<String> candidates) {
        return """
                女巫阶段。今晚%s被狼人击杀。
                你当前的药物状态：解药%s，毒药%s。
                存活玩家：%s
                
                请严格使用以下JSON格式回复：
                {"use_antidote": true或false, "use_poison": true或false, "poison_target": "毒药目标姓名或null"}
                
                规则：
                - 没有解药时 use_antidote 必须为 false
                - 没有毒药时 use_poison 必须为 false
                - 不使用毒药时 poison_target 为 null
                只输出JSON，不要输出其他内容。
                """.formatted(
                victim,
                hasAntidote ? "可用" : "已用",
                hasPoison ? "可用" : "已用",
                String.join("、", candidates));
    }

    /**
     * 创建游戏玩家列表
     */
    public static List<Player> createDefaultPlayers(SpringAIChatClient chatClient) {
        return List.of(
                createPlayer("孙权", GameRole.WEREWOLF, chatClient),
                createPlayer("周瑜", GameRole.WEREWOLF, chatClient),
                createPlayer("曹操", GameRole.SEER, chatClient),
                createPlayer("张飞", GameRole.WITCH, chatClient),
                createPlayer("司马懿", GameRole.VILLAGER, chatClient),
                createPlayer("赵云", GameRole.VILLAGER, chatClient)
        );
    }

    private static Player createPlayer(String character, GameRole role, SpringAIChatClient chatClient) {
        String systemPrompt = getRolePrompt(role, character);
        DialogAgent agent = new DialogAgent(character, systemPrompt, chatClient);
        return new Player(character, role, agent, true);
    }
}
