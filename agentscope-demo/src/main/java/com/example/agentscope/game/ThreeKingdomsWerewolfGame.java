package com.example.agentscope.game;

import com.example.agentscope.agent.DialogAgent;
import com.example.agentscope.chat.MsgHub;
import com.example.agentscope.chat.Pipeline;
import com.example.agentscope.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 三国狼人杀游戏控制器
 *
 * 对应教程中的 ThreeKingdomsWerewolfGame 类。
 * 分三层架构：
 * - 游戏控制层：维护全局状态、推进流程、裁定胜负
 * - 智能体交互层：通过 MsgHub 消息驱动
 * - 角色建模层：DialogAgent + 双重身份提示词
 */
public class ThreeKingdomsWerewolfGame {

    private static final Logger log = LoggerFactory.getLogger(ThreeKingdomsWerewolfGame.class);
    private static final int MAX_DISCUSSION_ROUND = 2;
    private static final Pattern JSON_TARGET_PATTERN = Pattern.compile("\"target\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_ANTIDOTE_PATTERN = Pattern.compile("\"use_antidote\"\\s*:\\s*(true|false)");
    private static final Pattern JSON_POISON_PATTERN = Pattern.compile("\"use_poison\"\\s*:\\s*(true|false)");
    private static final Pattern JSON_POISON_TARGET_PATTERN = Pattern.compile("\"poison_target\"\\s*:\\s*\"([^\"]+)\"");

    private final List<Player> players;
    private final DialogAgent moderator;
    private final Consumer<String> output;

    // 女巫药物状态
    private boolean witchHasAntidote = true;
    private boolean witchHasPoison = true;

    public ThreeKingdomsWerewolfGame(List<Player> players, DialogAgent moderator, Consumer<String> output) {
        this.players = new ArrayList<>(players);
        this.moderator = moderator;
        this.output = output;
    }

    /**
     * 运行完整游戏
     */
    public void run() {
        printLine("🎮 欢迎来到三国狼人杀！");
        printLine("");

        // 初始化：通知每位玩家各自的角色
        initializeGame();

        // 游戏循环
        int round = 0;
        while (!isGameOver()) {
            round++;
            printLine("=== 第%d轮游戏 ===".formatted(round));

            // 夜晚阶段
            nightPhase(round);
            if (isGameOver()) break;

            // 白天阶段
            dayPhase(round);
        }

        announceResult();
    }

    // ==================== 初始化 ====================

    private void initializeGame() {
        printLine("=== 游戏初始化 ===");

        for (Player player : players) {
            String roleAnnouncement = "📢 【%s】你在这场三国狼人杀中扮演%s，你的角色是%s。%s".formatted(
                    player.characterName(),
                    player.gameRole().displayName(),
                    player.characterName(),
                    player.gameRole().description());
            Msg announcement = new Msg("游戏主持人", roleAnnouncement, "system");
            player.agent().observe(announcement);
            printLine("游戏主持人: " + roleAnnouncement);
        }

        // 让狼人互相知道队友
        List<Player> werewolves = getAliveByRole(GameRole.WEREWOLF);
        if (werewolves.size() > 1) {
            String wolfNames = werewolves.stream().map(Player::characterName).collect(Collectors.joining("、"));
            for (Player wolf : werewolves) {
                Msg wolfInfo = new Msg("游戏主持人",
                        "你的狼人同伴是：%s。夜晚请协商行动。".formatted(wolfNames), "system");
                wolf.agent().observe(wolfInfo);
            }
        }

        String allPlayers = players.stream().map(Player::characterName).collect(Collectors.joining("、"));
        printLine("");
        printLine("游戏主持人: 📢 三国狼人杀游戏开始！参与者：" + allPlayers);
        printLine("✅ 游戏设置完成，共%d名玩家".formatted(players.size()));
        printLine("");
    }

    // ==================== 夜晚阶段 ====================

    private void nightPhase(int round) {
        printLine("🌙 第%d夜降临，天黑请闭眼...".formatted(round));
        printLine("");

        // 1. 狼人阶段
        String werewolfKill = werewolfPhase(round);

        // 2. 预言家阶段
        seerPhase(round);

        // 3. 女巫阶段
        String witchResult = witchPhase(round, werewolfKill);

        // 结算夜晚结果
        resolveNight(werewolfKill, witchResult);
    }

    /**
     * 狼人阶段 - 通过 MsgHub 建立狼人专属通信频道
     */
    private String werewolfPhase(int round) {
        printLine("【狼人阶段】");
        List<Player> werewolves = getAliveByRole(GameRole.WEREWOLF);
        if (werewolves.isEmpty()) return null;

        List<DialogAgent> wolfAgents = werewolves.stream().map(Player::agent).toList();
        List<String> candidates = getAliveGoodPlayerNames();

        Msg announcement = announce("🐺 狼人请睁眼，选择今晚要击杀的目标...");

        // 用 MsgHub 建立狼人专属通信频道（核心 AgentScope 机制）
        try (MsgHub wolfHub = MsgHub.create(wolfAgents, announcement)) {
            wolfHub.setMessageCallback(msg -> printLine(msg.name() + ": " + msg.content()));

            // 讨论阶段
            Msg discussionPrompt = announce(
                    "狼人们，请讨论今晚的击杀目标。存活玩家：%s".formatted(formatPlayerList()));
            for (int i = 0; i < MAX_DISCUSSION_ROUND; i++) {
                for (DialogAgent wolf : wolfAgents) {
                    try {
                        wolfHub.speak(wolf, discussionPrompt);
                    } catch (Exception e) {
                        printLine("⚠️ %s 讨论时出错: %s".formatted(wolf.getName(), e.getMessage()));
                    }
                }
            }

            // 投票阶段：关闭广播，独立投票
            wolfHub.setAutobroadcast(false);
            Msg killPrompt = announce("请选择击杀目标");
            String killFormat = RolePrompts.getWerewolfKillFormat(candidates);

            List<String> votes = new ArrayList<>();
            for (DialogAgent wolf : wolfAgents) {
                Msg vote = wolf.reply(new Msg("游戏主持人", killFormat, "system"));
                printLine(wolf.getName() + ": " + vote.content());
                String target = extractTarget(vote.content());
                if (target != null && candidates.contains(target)) {
                    votes.add(target);
                }
            }

            return getMostVoted(votes);
        }
    }

    /**
     * 预言家阶段 - 点对点查验
     */
    private void seerPhase(int round) {
        printLine("");
        printLine("【预言家阶段】");
        List<Player> seers = getAliveByRole(GameRole.SEER);
        if (seers.isEmpty()) {
            printLine("预言家已出局，跳过此阶段");
            return;
        }

        Player seer = seers.getFirst();
        announce("🔮 预言家请睁眼，选择要查验的玩家...");

        List<String> candidates = getAlivePlayerNames().stream()
                .filter(name -> !name.equals(seer.characterName()))
                .toList();

        String checkFormat = RolePrompts.getSeerCheckFormat(candidates);
        Msg response = seer.agent().reply(new Msg("游戏主持人", checkFormat, "system"));
        printLine(seer.characterName() + ": " + response.content());

        String target = extractTarget(response.content());
        if (target != null) {
            Player targetPlayer = findPlayer(target);
            if (targetPlayer != null) {
                String result = targetPlayer.gameRole() == GameRole.WEREWOLF ? "狼人" : "好人";
                Msg resultMsg = new Msg("游戏主持人",
                        "📢 查验结果：%s是%s".formatted(target, result), "system");
                seer.agent().observe(resultMsg);
                printLine("游戏主持人: " + resultMsg.content());
            }
        }
    }

    /**
     * 女巫阶段 - 决定是否使用药物
     */
    private String witchPhase(int round, String victim) {
        printLine("");
        printLine("【女巫阶段】");
        List<Player> witches = getAliveByRole(GameRole.WITCH);
        if (witches.isEmpty()) {
            printLine("女巫已出局，跳过此阶段");
            return null;
        }

        Player witch = witches.getFirst();
        announce("🧙‍♀️ 女巫请睁眼...");

        if (victim != null) {
            announce("今晚%s被狼人击杀".formatted(victim));
        }

        if (!witchHasAntidote && !witchHasPoison) {
            printLine("女巫已无药物可用，跳过此阶段");
            return null;
        }

        List<String> candidates = getAlivePlayerNames();
        String actionFormat = RolePrompts.getWitchActionFormat(
                victim != null ? victim : "无人",
                witchHasAntidote, witchHasPoison, candidates);

        Msg response = witch.agent().reply(new Msg("游戏主持人", actionFormat, "system"));
        printLine(witch.characterName() + ": " + response.content());

        // 解析女巫行动
        String content = response.content();
        boolean useAntidote = extractBoolean(content, JSON_ANTIDOTE_PATTERN);
        boolean usePoison = extractBoolean(content, JSON_POISON_PATTERN);

        // 使用解药
        if (useAntidote && witchHasAntidote && victim != null) {
            witchHasAntidote = false;
            Msg saveMsg = new Msg("游戏主持人",
                    "📢 你使用解药救了%s".formatted(victim), "system");
            witch.agent().observe(saveMsg);
            printLine("游戏主持人: " + saveMsg.content());
            return "saved:" + victim;
        }

        // 使用毒药
        if (usePoison && witchHasPoison) {
            String poisonTarget = extractTarget(content, JSON_POISON_TARGET_PATTERN);
            if (poisonTarget != null) {
                witchHasPoison = false;
                Msg poisonMsg = new Msg("游戏主持人",
                        "📢 你对%s使用了毒药".formatted(poisonTarget), "system");
                witch.agent().observe(poisonMsg);
                printLine("游戏主持人: " + poisonMsg.content());
                return "poison:" + poisonTarget;
            }
        }

        return null;
    }

    /**
     * 结算夜晚结果
     */
    private void resolveNight(String werewolfKill, String witchResult) {
        printLine("");
        List<String> killed = new ArrayList<>();

        // 判断狼人击杀是否被女巫救了
        boolean saved = witchResult != null && witchResult.startsWith("saved:");
        if (werewolfKill != null && !saved) {
            killed.add(werewolfKill);
        }

        // 女巫毒杀
        if (witchResult != null && witchResult.startsWith("poison:")) {
            String poisonVictim = witchResult.substring("poison:".length());
            if (!killed.contains(poisonVictim)) {
                killed.add(poisonVictim);
            }
        }

        // 执行死亡
        for (String victimName : killed) {
            killPlayer(victimName);
        }

        // 广播结果
        if (killed.isEmpty()) {
            Msg resultMsg = announce("昨夜平安无事，无人死亡。");
            broadcastToAll(resultMsg);
        } else {
            String deathMsg = "昨夜 %s 不幸遇害。".formatted(String.join("、", killed));
            Msg resultMsg = announce(deathMsg);
            broadcastToAll(resultMsg);
        }

        printLine("");
    }

    // ==================== 白天阶段 ====================

    private void dayPhase(int round) {
        printLine("【白天讨论阶段】");
        List<Player> alive = getAlivePlayers();
        List<DialogAgent> aliveAgents = alive.stream().map(Player::agent).toList();

        Msg dayAnnouncement = announce("☀️ 第%d天天亮了，请大家睁眼...".formatted(round));
        Msg discussionAnnouncement = announce(
                "现在开始自由讨论。存活玩家：%s".formatted(formatPlayerList()));

        // 通过 MsgHub 建立全体讨论频道
        try (MsgHub dayHub = MsgHub.create(aliveAgents, dayAnnouncement)) {
            dayHub.setMessageCallback(msg -> printLine(msg.name() + ": " + msg.content()));
            dayHub.broadcast(discussionAnnouncement);

            // 顺序讨论（sequential_pipeline）
            Pipeline.sequential(dayHub, aliveAgents, discussionAnnouncement);
        }

        // 投票阶段
        printLine("");
        printLine("【投票阶段】");
        Msg voteAnnouncement = announce("请投票选择要淘汰的玩家");

        List<String> candidates = getAlivePlayerNames();
        String voteFormat = RolePrompts.getVoteFormat(candidates);

        // 扇出投票（fanout_pipeline）- 独立投票，互不影响
        List<Msg> voteMessages = Pipeline.fanout(aliveAgents,
                new Msg("游戏主持人", voteFormat, "system"));

        // 统计投票
        List<String> votes = new ArrayList<>();
        for (Msg voteMsg : voteMessages) {
            printLine(voteMsg.name() + ": " + voteMsg.content());
            String target = extractTarget(voteMsg.content());
            if (target != null && candidates.contains(target)) {
                votes.add(target);
            }
        }

        String eliminated = getMostVoted(votes);
        if (eliminated != null) {
            killPlayer(eliminated);
            Msg elimMsg = announce("投票结果：%s 被投票淘汰。".formatted(eliminated));
            broadcastToAll(elimMsg);
        } else {
            Msg noElimMsg = announce("投票未达成一致，本轮无人被淘汰。");
            broadcastToAll(noElimMsg);
        }

        printLine("");
    }

    // ==================== 胜负判定 ====================

    private boolean isGameOver() {
        long aliveWerewolves = getAlivePlayers().stream()
                .filter(p -> p.gameRole() == GameRole.WEREWOLF).count();
        long aliveGood = getAlivePlayers().stream()
                .filter(p -> p.gameRole().isGood()).count();

        if (aliveWerewolves == 0) return true;  // 好人胜利
        if (aliveWerewolves >= aliveGood) return true;  // 狼人胜利
        return false;
    }

    private void announceResult() {
        printLine("=".repeat(50));
        long aliveWerewolves = getAlivePlayers().stream()
                .filter(p -> p.gameRole() == GameRole.WEREWOLF).count();

        if (aliveWerewolves == 0) {
            printLine("🎉 好人阵营获胜！所有狼人已被消灭！");
        } else {
            printLine("🐺 狼人阵营获胜！狼人成功控制了局面！");
        }

        printLine("");
        printLine("📋 最终玩家状态：");
        for (Player player : players) {
            String status = player.alive() ? "存活 ✅" : "死亡 ❌";
            printLine("  %s（%s）：%s".formatted(
                    player.characterName(), player.gameRole().displayName(), status));
        }
    }

    // ==================== 工具方法 ====================

    private Msg announce(String content) {
        Msg msg = new Msg("游戏主持人", "📢 " + content, "system");
        printLine("游戏主持人: " + msg.content());
        return msg;
    }

    private void broadcastToAll(Msg message) {
        for (Player player : getAlivePlayers()) {
            player.agent().observe(message);
        }
    }

    private List<Player> getAlivePlayers() {
        return players.stream().filter(Player::alive).toList();
    }

    private List<Player> getAliveByRole(GameRole role) {
        return players.stream()
                .filter(p -> p.alive() && p.gameRole() == role)
                .toList();
    }

    private List<String> getAlivePlayerNames() {
        return getAlivePlayers().stream().map(Player::characterName).toList();
    }

    private List<String> getAliveGoodPlayerNames() {
        return getAlivePlayers().stream()
                .filter(p -> p.gameRole().isGood())
                .map(Player::characterName)
                .toList();
    }

    private String formatPlayerList() {
        return getAlivePlayers().stream()
                .map(Player::characterName)
                .collect(Collectors.joining("、"));
    }

    private Player findPlayer(String name) {
        return players.stream()
                .filter(p -> p.characterName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void killPlayer(String name) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).characterName().equals(name) && players.get(i).alive()) {
                players.set(i, players.get(i).killed());
                log.info("{} 已死亡", name);
                return;
            }
        }
    }

    private String extractTarget(String content) {
        return extractTarget(content, JSON_TARGET_PATTERN);
    }

    private String extractTarget(String content, Pattern pattern) {
        if (content == null) return null;
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean extractBoolean(String content, Pattern pattern) {
        if (content == null) return false;
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return "true".equals(matcher.group(1));
        }
        return false;
    }

    /**
     * 统计投票，返回得票最多的目标
     */
    private String getMostVoted(List<String> votes) {
        if (votes.isEmpty()) return null;

        Map<String, Long> counts = votes.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void printLine(String text) {
        output.accept(text);
    }
}
