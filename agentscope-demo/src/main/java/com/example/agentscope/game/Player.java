package com.example.agentscope.game;

import com.example.agentscope.agent.DialogAgent;

/**
 * 游戏玩家 - 将三国人物与狼人杀角色绑定
 *
 * 双重身份设计：
 * - 游戏功能角色（狼人、预言家、女巫、村民）
 * - 文化人格角色（刘备、曹操、诸葛亮等三国人物）
 */
public record Player(
        String characterName,  // 三国人物名称
        GameRole gameRole,     // 狼人杀角色
        DialogAgent agent,     // 关联的智能体
        boolean alive          // 是否存活
) {
    /**
     * 创建已死亡副本
     */
    public Player killed() {
        return new Player(characterName, gameRole, agent, false);
    }

    /**
     * 创建被救活副本
     */
    public Player saved() {
        return new Player(characterName, gameRole, agent, true);
    }
}
