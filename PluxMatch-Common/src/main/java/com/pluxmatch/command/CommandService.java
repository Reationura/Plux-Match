package com.pluxmatch.command;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.database.LeaderboardEntry;
import com.pluxmatch.match.MatchManager;
import com.pluxmatch.model.MatchRecord;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.model.Queue;
import com.pluxmatch.model.QueueResult;
import com.pluxmatch.model.ServerData;
import com.pluxmatch.party.PartyManager;
import com.pluxmatch.platform.CommandSource;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.queue.QueueManager;
import com.pluxmatch.server.ServerManager;
import com.pluxmatch.tier.TierManager;
import com.pluxmatch.util.MapUtils;
import com.pluxmatch.util.MessageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 跨平台命令逻辑（下沉至 Common），Bungee / Velocity 适配层仅转调本类。
 *
 * @author Reationura
 */
public class CommandService {

    private final PluxMatchCore core;
    private final PlatformBridge bridge;
    private final ConfigManager configManager;
    private final MessageManager messages;
    private final QueueManager queueManager;
    private final PartyManager partyManager;
    private final ServerManager serverManager;
    private final MatchManager matchManager;
    private final TierManager tierManager;

    public CommandService(PluxMatchCore core) {
        this.core = core;
        this.bridge = core.bridge();
        this.configManager = core.getConfigManager();
        this.messages = core.getMessageManager();
        this.queueManager = core.getQueueManager();
        this.partyManager = core.getPartyManager();
        this.serverManager = core.getServerManager();
        this.matchManager = core.getMatchManager();
        this.tierManager = core.getTierManager();
    }

    /* ==================== /match ==================== */

    public void executeMatch(CommandSource sender, String[] args) {
        if (!(sender.isPlayer())) {
            messages.sendMessage(sender, "general.only-player");
            return;
        }
        MatchPlayer player = sender.asPlayer();

        if (args.length == 0) {
            handleHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                handleJoin(sender, player, args);
                break;
            case "leave":
                handleLeave(sender, player);
                break;
            case "status":
                handleStatus(sender, player);
                break;
            case "party":
                handleParty(sender, player, args);
                break;
            case "history":
                handleHistory(sender, player);
                break;
            case "stats":
                handleStats(sender, player);
                break;
            case "leaderboard":
                handleLeaderboard(sender, args);
                break;
            case "gui":
                handleGUI(sender, player, args);
                break;
            case "help":
                handleHelp(sender);
                break;
            default:
                messages.sendMessage(sender, "general.unknown-command");
        }
    }

    private void handleJoin(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }

        String queueId = args[1];
        Queue queue = queueManager.getQueue(queueId);

        if (queue == null) {
            Map<String, Object> queuesConfig = configManager.getQueues();
            Map<String, Object> queueGroups = configManager.getSection(queuesConfig, "queue-groups");

            if (queueGroups != null && queueGroups.containsKey(queueId)) {
                for (String groupQueueId : configManager.getStringList(queueGroups, queueId + ".queues")) {
                    queue = queueManager.getQueue(groupQueueId);
                    if (queue != null && queue.isEnabled()) {
                        queueId = groupQueueId;
                        break;
                    }
                }
            }
        }

        if (queue == null) {
            messages.sendMessage(sender, "queue.queue-not-found", MapUtils.of("queue", queueId));
            return;
        }

        QueueResult result = queueManager.joinQueue(player.getUniqueId(), player.getName(), queueId);
        sendQueueResult(sender, result, queue.getName());
    }

    private void sendQueueResult(CommandSource sender, QueueResult result, String queueName) {
        switch (result) {
            case SUCCESS:
                messages.sendMessage(sender, "queue.join-success", MapUtils.of("queue", queueName));
                break;
            case QUEUE_NOT_FOUND:
                messages.sendMessage(sender, "queue.queue-not-found", MapUtils.of("queue", queueName));
                break;
            case QUEUE_DISABLED:
                messages.sendMessage(sender, "queue.queue-disabled", MapUtils.of("queue", queueName));
                break;
            case ALREADY_IN_QUEUE:
                messages.sendMessage(sender, "queue.already-in-queue");
                break;
            case COOLDOWN:
                messages.sendMessage(sender, "queue.join-cooldown");
                break;
            case QUEUE_FULL:
                messages.sendMessage(sender, "queue.queue-full");
                break;
            case NO_PERMISSION:
                messages.sendMessage(sender, "general.no-permission");
                break;
            case PARTY_NOT_ALLOWED:
                messages.sendMessage(sender, "queue.party-not-allowed");
                break;
            case PARTY_SIZE_MISMATCH:
                messages.sendMessage(sender, "queue.party-size-mismatch");
                break;
            default:
                messages.sendMessage(sender, "general.unknown-command");
        }
    }

    private void handleLeave(CommandSource sender, MatchPlayer player) {
        if (queueManager.leaveQueue(player.getUniqueId())) {
            messages.sendMessage(sender, "queue.leave-success");
        } else {
            messages.sendMessage(sender, "queue.not-in-queue");
        }
    }

    private void handleStatus(CommandSource sender, MatchPlayer player) {
        String queueId = queueManager.getPlayerQueue(player.getUniqueId());
        if (queueId == null) {
            messages.sendMessage(sender, "queue.not-in-queue");
            return;
        }

        Queue queue = queueManager.getQueue(queueId);
        if (queue == null) {
            return;
        }

        int position = queueManager.getQueuePosition(player.getUniqueId());
        int total = queue.getCurrentSize();
        messages.sendMessage(sender, "queue.position-update",
            MapUtils.of("position", String.valueOf(position), "total", String.valueOf(total)));

        core.getGuiManager().openMatchStatus(player);
    }

    private void handleParty(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 2) {
            core.getGuiManager().openPartyMenu(player);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create":
                handlePartyCreate(sender, player);
                break;
            case "invite":
                handlePartyInvite(sender, player, args);
                break;
            case "accept":
                handlePartyAccept(sender, player, args);
                break;
            case "deny":
                handlePartyDeny(sender, player, args);
                break;
            case "kick":
                handlePartyKick(sender, player, args);
                break;
            case "disband":
                handlePartyDisband(sender, player);
                break;
            case "list":
                handlePartyList(sender, player);
                break;
            case "join":
                handlePartyJoinQueue(sender, player, args);
                break;
            default:
                messages.sendMessage(sender, "general.unknown-command");
        }
    }

    private void handlePartyCreate(CommandSource sender, MatchPlayer player) {
        if (partyManager.isInParty(player.getUniqueId())) {
            messages.sendMessage(sender, "party.already-in-party");
            return;
        }
        partyManager.createParty(player.getUniqueId());
        messages.sendMessage(sender, "party.create-success");
    }

    private void handlePartyInvite(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        MatchPlayer target = bridge.player(args[2]).orElse(null);
        if (target == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[2]));
            return;
        }
        if (partyManager.invitePlayer(player.getUniqueId(), target.getUniqueId())) {
            messages.sendMessage(sender, "party.invite-sent", MapUtils.of("player", target.getName()));
        } else {
            messages.sendMessage(sender, "party.not-leader");
        }
    }

    private void handlePartyAccept(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        MatchPlayer inviter = bridge.player(args[2]).orElse(null);
        if (inviter == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[2]));
            return;
        }
        if (!partyManager.acceptInvitation(player.getUniqueId(), inviter.getUniqueId())) {
            messages.sendMessage(sender, "party.invite-expired");
        }
    }

    private void handlePartyDeny(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        MatchPlayer inviter = bridge.player(args[2]).orElse(null);
        if (inviter == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[2]));
            return;
        }
        partyManager.denyInvitation(player.getUniqueId(), inviter.getUniqueId());
    }

    private void handlePartyKick(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        MatchPlayer target = bridge.player(args[2]).orElse(null);
        if (target == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[2]));
            return;
        }
        if (partyManager.kickMember(player.getUniqueId(), target.getUniqueId())) {
            messages.sendMessage(sender, "party.kick-success", MapUtils.of("player", target.getName()));
        } else {
            messages.sendMessage(sender, "party.not-leader");
        }
    }

    private void handlePartyDisband(CommandSource sender, MatchPlayer player) {
        if (!partyManager.disbandParty(player.getUniqueId())) {
            messages.sendMessage(sender, "party.not-leader");
        }
    }

    private void handlePartyList(CommandSource sender, MatchPlayer player) {
        Set<UUID> members = partyManager.getMembers(player.getUniqueId());
        if (members.isEmpty()) {
            messages.sendMessage(sender, "party.not-in-party");
            return;
        }

        StringBuilder sb = new StringBuilder("&6队伍成员: ");
        for (UUID member : members) {
            MatchPlayer p = bridge.player(member).orElse(null);
            String name = p != null ? p.getName() : "离线";
            sb.append(partyManager.isLeader(member) ? "&c" : "&a").append(name).append("&7, ");
        }
        messages.sendRaw(sender, sb.toString());
    }

    private void handlePartyJoinQueue(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 3) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        String queueId = args[2];
        Queue queue = queueManager.getQueue(queueId);
        if (queue == null) {
            messages.sendMessage(sender, "queue.queue-not-found", MapUtils.of("queue", queueId));
            return;
        }

        QueueResult result = queueManager.joinQueue(player.getUniqueId(), player.getName(), queueId);
        if (result == QueueResult.SUCCESS) {
            messages.sendMessage(sender, "party.party-join-queue");
        } else {
            sendQueueResult(sender, result, queue.getName());
        }
    }

    private void handleHistory(CommandSource sender, MatchPlayer player) {
        List<MatchRecord> records = core.getDatabaseManager().getPlayerMatchHistory(player.getUniqueId(), 10);
        if (records.isEmpty()) {
            messages.sendRaw(sender, "&c暂无对局记录");
            return;
        }
        core.getGuiManager().openHistoryMenu(player);
    }

    private void handleStats(CommandSource sender, MatchPlayer player) {
        PlayerData data = core.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (data == null) {
            data = new PlayerData(player.getUniqueId(), player.getName());
        }
        sendStatsCard(sender, data);
    }

    private void sendStatsCard(CommandSource sender, PlayerData data) {
        String tierName = data.getTier();
        String tierDisplay = tierManager.getDisplayName(tierName);
        messages.sendRaw(sender, "&6=== 匹配统计 ===");
        messages.sendRaw(sender, "&7ELO: &a" + data.getElo());
        messages.sendRaw(sender, "&7段位: " + tierDisplay);
        messages.sendRaw(sender, "&a胜场: " + data.getWins());
        messages.sendRaw(sender, "&c败场: " + data.getLosses());
        messages.sendRaw(sender, "&7胜率: " + String.format("%.1f", data.getWinRate()) + "%");
        messages.sendRaw(sender, "&7总场次: " + data.getTotalMatches());
    }

    private void handleLeaderboard(CommandSource sender, String[] args) {
        boolean byWinRate = args.length >= 2 && args[1].equalsIgnoreCase("winrate");
        int limit = configManager.getInt(configManager.getConfig(), "leaderboard.size", 10);

        List<LeaderboardEntry> top = core.getDatabaseManager().getTopPlayers(byWinRate, limit);
        if (top.isEmpty()) {
            messages.sendRaw(sender, "&c暂无排行数据");
            return;
        }

        messages.sendRaw(sender, "&6=== 排行榜 (" + (byWinRate ? "胜率" : "ELO") + ") ===");
        int rank = 1;
        for (LeaderboardEntry e : top) {
            String line = "&e#" + rank + " &6" + e.name
                + " &7ELO:&a " + e.elo
                + " &7W/L:&f " + e.wins + "/" + e.losses
                + " &7胜率:&f " + String.format("%.1f", e.winRate()) + "%";
            messages.sendRaw(sender, line);
            rank++;
        }
    }

    private void handleGUI(CommandSource sender, MatchPlayer player, String[] args) {
        if (args.length < 2) {
            messages.sendMessage(sender, "general.unknown-command");
            return;
        }
        core.getGuiManager().openQueueGroupGUI(player, args[1].toLowerCase());
    }

    private void handleHelp(CommandSource sender) {
        messages.sendRaw(sender, "&6=== PluxMatch 帮助 ===");
        messages.sendRaw(sender, "&a/match - 查看帮助");
        messages.sendRaw(sender, "&a/match gui <组名称> - 打开指定组的匹配GUI");
        messages.sendRaw(sender, "&a/match join <队列> - 加入队列");
        messages.sendRaw(sender, "&a/match leave - 退出队列");
        messages.sendRaw(sender, "&a/match status - 查看匹配状态");
        messages.sendRaw(sender, "&a/match party create|invite|accept|deny|kick|disband|list|join - 队伍操作");
        messages.sendRaw(sender, "&a/match history - 查看对局记录");
        messages.sendRaw(sender, "&a/match stats - 查看个人统计");
        messages.sendRaw(sender, "&a/match leaderboard [winrate] - 查看排行榜");
    }

    /* ==================== /matchadmin ==================== */

    public void executeMatchAdmin(CommandSource sender, String[] args) {
        if (!sender.hasPermission("match.admin")) {
            messages.sendMessage(sender, "general.no-permission");
            return;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "queue":
                handleQueueAdmin(sender, args);
                break;
            case "add":
                handleAddPlayer(sender, args);
                break;
            case "remove":
                handleRemovePlayer(sender, args);
                break;
            case "server":
                handleServerAdmin(sender, args);
                break;
            case "forcestart":
                handleForceStart(sender, args);
                break;
            case "stats":
                handlePlayerStats(sender, args);
                break;
            default:
                sendAdminHelp(sender);
        }
    }

    private void sendAdminHelp(CommandSource sender) {
        messages.sendRaw(sender, "&6=== PluxMatch 管理员帮助 ===");
        messages.sendRaw(sender, "&a/matchadmin reload - 重载配置");
        messages.sendRaw(sender, "&a/matchadmin queue create|delete|enable|disable|list <名称>");
        messages.sendRaw(sender, "&a/matchadmin add <玩家> <队列> - 强制加入");
        messages.sendRaw(sender, "&a/matchadmin remove <玩家> - 强制移出");
        messages.sendRaw(sender, "&a/matchadmin server add <服务器> <组名> | remove <服务器> | list");
        messages.sendRaw(sender, "&a/matchadmin forcestart <队列> - 强制匹配");
        messages.sendRaw(sender, "&a/matchadmin stats <玩家> - 查看统计");
    }

    private void handleReload(CommandSource sender) {
        core.reload();
        messages.sendMessage(sender, "general.reload-success");
    }

    private void handleQueueAdmin(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create":
                handleQueueCreate(sender, args);
                break;
            case "delete":
                handleQueueDelete(sender, args);
                break;
            case "enable":
                handleQueueEnable(sender, args, true);
                break;
            case "disable":
                handleQueueEnable(sender, args, false);
                break;
            case "list":
                handleQueueList(sender);
                break;
            default:
                sendAdminHelp(sender);
        }
    }

    private void handleQueueCreate(CommandSource sender, String[] args) {
        if (args.length < 3) {
            messages.sendRaw(sender, "&c用法: /matchadmin queue create <名称>");
            return;
        }
        String queueId = args[2];
        if (queueManager.getQueue(queueId) != null) {
            messages.sendRaw(sender, "&c队列已存在: " + queueId);
            return;
        }

        Queue queue = new Queue(queueId, queueId, "", true, "default", 2, 16, 1,
            Queue.AlgorithmType.FIFO, Queue.PopulationMode.RANDOM, false, false, 10,
            "match.queue." + queueId, queueId, false);
        queueManager.createQueue(queue);
        messages.sendMessage(sender, "admin.queue-created", MapUtils.of("queue", queueId));
    }

    private void handleQueueDelete(CommandSource sender, String[] args) {
        if (args.length < 3) {
            messages.sendRaw(sender, "&c用法: /matchadmin queue delete <名称>");
            return;
        }
        String queueId = args[2];
        if (queueManager.getQueue(queueId) == null) {
            messages.sendRaw(sender, "&c队列不存在: " + queueId);
            return;
        }
        queueManager.deleteQueue(queueId);
        messages.sendMessage(sender, "admin.queue-deleted", MapUtils.of("queue", queueId));
    }

    private void handleQueueEnable(CommandSource sender, String[] args, boolean enable) {
        if (args.length < 3) {
            messages.sendRaw(sender, "&c用法: /matchadmin queue " + (enable ? "enable" : "disable") + " <名称>");
            return;
        }
        String queueId = args[2];
        Queue queue = queueManager.getQueue(queueId);
        if (queue == null) {
            messages.sendRaw(sender, "&c队列不存在: " + queueId);
            return;
        }
        queue.setEnabled(enable);
        messages.sendMessage(sender, enable ? "admin.queue-enabled" : "admin.queue-disabled",
            MapUtils.of("queue", queueId));
    }

    private void handleQueueList(CommandSource sender) {
        Collection<Queue> queues = queueManager.getQueues();
        if (queues.isEmpty()) {
            messages.sendMessage(sender, "admin.no-queues");
            return;
        }
        messages.sendRaw(sender, "&6=== 队列列表 ===");
        for (Queue queue : queues) {
            messages.sendRaw(sender, "&a" + queue.getId() + " &7- " + queue.getName()
                + " (" + (queue.isEnabled() ? "&a启用" : "&c禁用") + "&7) "
                + "&7当前: " + queue.getCurrentSize() + "/" + queue.getMaxPlayers());
        }
    }

    private void handleAddPlayer(CommandSource sender, String[] args) {
        if (args.length < 3) {
            messages.sendRaw(sender, "&c用法: /matchadmin add <玩家> <队列>");
            return;
        }
        MatchPlayer player = bridge.player(args[1]).orElse(null);
        if (player == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[1]));
            return;
        }
        Queue queue = queueManager.getQueue(args[2]);
        if (queue == null) {
            messages.sendMessage(sender, "queue.queue-not-found", MapUtils.of("queue", args[2]));
            return;
        }
        QueueResult result = queueManager.joinQueue(player.getUniqueId(), player.getName(), args[2]);
        if (result == QueueResult.SUCCESS) {
            messages.sendMessage(sender, "admin.player-added", MapUtils.of("player", player.getName()));
        } else {
            sendQueueResult(sender, result, queue.getName());
        }
    }

    private void handleRemovePlayer(CommandSource sender, String[] args) {
        if (args.length < 2) {
            messages.sendRaw(sender, "&c用法: /matchadmin remove <玩家>");
            return;
        }
        MatchPlayer player = bridge.player(args[1]).orElse(null);
        if (player == null) {
            messages.sendMessage(sender, "general.player-not-found", MapUtils.of("player", args[1]));
            return;
        }
        if (queueManager.leaveQueue(player.getUniqueId())) {
            messages.sendMessage(sender, "admin.player-removed", MapUtils.of("player", player.getName()));
        } else {
            messages.sendRaw(sender, "&c玩家不在队列中");
        }
    }

    private void handleServerAdmin(CommandSource sender, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender);
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add":
                if (args.length < 4) {
                    messages.sendRaw(sender, "&c用法: /matchadmin server add <服务器> <组名>");
                    return;
                }
                if (serverManager.addServerToGroup(args[2], args[3])) {
                    messages.sendMessage(sender, "admin.server-added", MapUtils.of("server", args[2]));
                } else {
                    messages.sendRaw(sender, "&c服务器已在组中");
                }
                break;
            case "remove":
                if (args.length < 3) {
                    messages.sendRaw(sender, "&c用法: /matchadmin server remove <服务器>");
                    return;
                }
                if (serverManager.removeServerFromGroup(args[2])) {
                    messages.sendMessage(sender, "admin.server-removed", MapUtils.of("server", args[2]));
                } else {
                    messages.sendRaw(sender, "&c服务器不存在");
                }
                break;
            case "list":
                handleServerList(sender);
                break;
            default:
                sendAdminHelp(sender);
        }
    }

    private void handleServerList(CommandSource sender) {
        Collection<ServerData> servers = serverManager.getServers();
        if (servers.isEmpty()) {
            messages.sendMessage(sender, "admin.no-servers");
            return;
        }
        messages.sendRaw(sender, "&6=== 服务器列表 ===");
        for (ServerData server : servers) {
            messages.sendRaw(sender, "&a" + server.getName() + " &7- " + server.getGroup()
                + " (" + (server.isOnline() ? "&a在线" : "&c离线") + "&7) "
                + "&7人数: " + server.getCurrentPlayers() + "/" + server.getMaxPlayers());
        }
    }

    private void handleForceStart(CommandSource sender, String[] args) {
        if (args.length < 2) {
            messages.sendRaw(sender, "&c用法: /matchadmin forcestart <队列>");
            return;
        }
        Queue queue = queueManager.getQueue(args[1]);
        if (queue == null) {
            messages.sendMessage(sender, "queue.queue-not-found", MapUtils.of("queue", args[1]));
            return;
        }
        messages.sendMessage(sender, "admin.force-start");
        matchManager.processMatches();
    }

    private void handlePlayerStats(CommandSource sender, String[] args) {
        if (args.length < 2) {
            messages.sendRaw(sender, "&c用法: /matchadmin stats <玩家>");
            return;
        }
        MatchPlayer player = bridge.player(args[1]).orElse(null);
        if (player == null) {
            messages.sendRaw(sender, "&c玩家不在线");
            return;
        }
        PlayerData data = core.getDatabaseManager().loadPlayer(player.getUniqueId());
        if (data == null) {
            messages.sendRaw(sender, "&c未找到玩家数据");
            return;
        }
        messages.sendRaw(sender, "&6=== " + player.getName() + " 的统计 ===");
        sendStatsCard(sender, data);
    }

    /* ==================== Tab 补全 ==================== */

    public List<String> tabCompleteMatch(CommandSource sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("join", "leave", "status", "party", "history", "stats", "leaderboard", "gui", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            completions.addAll(queueManager.getEnabledQueues().stream().map(Queue::getId).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gui")) {
            completions.addAll(getQueueGroups());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("party")) {
            completions.addAll(Arrays.asList("create", "invite", "accept", "deny", "kick", "disband", "list", "join"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("party")) {
            String sub = args[1].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("accept") || sub.equals("deny")) {
                completions.addAll(bridge.onlinePlayerNames());
            } else if (sub.equals("join")) {
                completions.addAll(queueManager.getEnabledQueues().stream().map(Queue::getId).collect(Collectors.toList()));
            }
        }

        return filter(completions, args);
    }

    public List<String> tabCompleteMatchAdmin(CommandSource sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("match.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "queue", "add", "remove", "server", "forcestart", "stats"));
        } else if (args.length == 2) {
            String root = args[0].toLowerCase();
            if (root.equals("queue")) {
                completions.addAll(Arrays.asList("create", "delete", "enable", "disable", "list"));
            } else if (root.equals("server")) {
                completions.addAll(Arrays.asList("add", "remove", "list"));
            } else if (root.equals("add") || root.equals("remove") || root.equals("stats")) {
                completions.addAll(bridge.onlinePlayerNames());
            } else if (root.equals("forcestart")) {
                completions.addAll(queueManager.getEnabledQueues().stream().map(Queue::getId).collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            String root = args[0].toLowerCase();
            if (root.equals("queue") && !args[1].equalsIgnoreCase("create") && !args[1].equalsIgnoreCase("list")) {
                completions.addAll(queueManager.getQueues().stream().map(Queue::getId).collect(Collectors.toList()));
            } else if (root.equals("add")) {
                completions.addAll(queueManager.getEnabledQueues().stream().map(Queue::getId).collect(Collectors.toList()));
            } else if (root.equals("server")) {
                completions.addAll(serverManager.getServers().stream().map(ServerData::getName).collect(Collectors.toList()));
            }
        }

        return filter(completions, args);
    }

    private List<String> getQueueGroups() {
        List<String> groups = new ArrayList<>();
        Map<String, Object> queuesConfig = configManager.getQueues();
        Map<String, Object> groupsSection = configManager.getSection(queuesConfig, "queue-groups");
        if (groupsSection != null) {
            groups.addAll(groupsSection.keySet());
        }
        return groups;
    }

    private List<String> filter(List<String> completions, String[] args) {
        String last = args.length > 0 ? args[args.length - 1].toLowerCase() : "";
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(last))
            .distinct()
            .collect(Collectors.toList());
    }
}
