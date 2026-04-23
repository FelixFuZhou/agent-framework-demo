package com.example.agentscope.game;

/**
 * 游戏角色枚举 - 狼人杀中的功能性角色
 */
public enum GameRole {
    WEREWOLF("狼人", "夜晚可以击杀一名玩家"),
    SEER("预言家", "夜晚可以查验一名玩家的身份"),
    WITCH("女巫", "拥有一瓶解药和一瓶毒药"),
    VILLAGER("村民", "没有特殊能力，依靠推理投票");

    private final String displayName;
    private final String description;

    GameRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /**
     * 是否属于好人阵营
     */
    public boolean isGood() {
        return this != WEREWOLF;
    }
}
