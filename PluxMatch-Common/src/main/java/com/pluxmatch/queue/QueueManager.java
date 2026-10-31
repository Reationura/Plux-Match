package com.pluxmatch.queue;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.event.PlayerQueueJoinEvent;
import com.pluxmatch.event.PlayerQueueLeaveEvent;
import com.pluxmatch.model.Party;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.model.Queue;
import com.pluxmatch.model.QueueEntry;
import com.pluxmatch.model.QueueResult;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.redis.Protocol;
import com.pluxmatch.redis.RedisManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 队列管理（平台无关）。
 *
 * @author Reationura
 */
public class QueueManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final PlatformBridge bridge;

    private final ConcurrentHashMap<String, Queue> queues;
    private final ConcurrentHashMap<UUID, String> playerQueueMap;
    private final ConcurrentHashMap<UUID, Long> joinCooldown;

    private ScheduledExecutorService scheduler;

    public QueueManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        this.bridge = core.bridge();

        this.queues = new ConcurrentHashMap<>();
        this.playerQueueMap = new ConcurrentHashMap<>();
        this.joinCooldown = new ConcurrentHashMap<>();

        loadQueues();
        startScheduler();
    }

    private void loadQueues() {
        Map<String, Object> queuesConfig = configManager.getQueues();
        Map<String, Object> queuesSection = configManager.getSection(queuesConfig, "queues");

        for (Map.Entry<String, Object> entry : queuesSection.entrySet()) {
            String queueId = entry.getKey();
            Map<String, Object> queueData = configManager.getSection(queuesConfig, "queues." + queueId);

            Queue.AlgorithmType algorithm = Queue.AlgorithmType.valueOf(
                configManager.getString(queueData, "algorithm", "FIFO").toUpperCase());
            Queue.PopulationMode populationMode = Queue.PopulationMode.valueOf(
                configManager.getString(queueData, "population-mode", "RANDOM").toUpperCase());

            boolean isSpecial = configManager.getBoolean(queueData, "special", false);
            String joinPermission = configManager.getString(queueData, "join-permission", null);

            if (joinPermission == null) {
                joinPermission = isSpecial ? "match.spqueue." + queueId : "match.queue";
            }

            Queue queue = new Queue(
                queueId,
                configManager.getString(queueData, "name", queueId),
                configManager.getString(queueData, "description", ""),
                configManager.getBoolean(queueData, "enabled", true),
                configManager.getString(queueData, "group", "default"),
                configManager.getInt(queueData, "min-players", 2),
                configManager.getInt(queueData, "max-players", 16),
                configManager.getInt(queueData, "team-size", 1),
                algorithm,
                populationMode,
                configManager.getBoolean(queueData, "allow-party", false),
                configManager.getBoolean(queueData, "hybrid-mode", false),
                configManager.getInt(queueData, "timeout", 10),
                joinPermission,
                configManager.getString(queueData, "server-group", queueId),
                isSpecial
            );

            queues.put(queueId, queue);
            core.logger().info("Loaded queue: " + queue.getName() + " (special: " + isSpecial + ")");
        }
    }

    private void startScheduler() {
        scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "PluxMatch-Queue-Scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::updatePositions, 0, 1, TimeUnit.SECONDS);
    }

    public QueueResult joinQueue(UUID playerId, String playerName, String queueId) {
        Queue queue = queues.get(queueId);
        if (queue == null) {
            return QueueResult.QUEUE_NOT_FOUND;
        }

        if (!queue.isEnabled()) {
            return QueueResult.QUEUE_DISABLED;
        }

        if (playerQueueMap.containsKey(playerId)) {
            return QueueResult.ALREADY_IN_QUEUE;
        }

        if (isOnCooldown(playerId)) {
            return QueueResult.COOLDOWN;
        }

        if (queue.getCurrentSize() >= queue.getMaxPlayers()) {
            return QueueResult.QUEUE_FULL;
        }

        Party party = core.getPartyManager().getParty(playerId);
        int partySize = party != null ? party.getSize() : 1;
        UUID partyLeaderId = party != null ? party.getLeader() : null;

        if (party != null && !queue.isAllowParty()) {
            return QueueResult.PARTY_NOT_ALLOWED;
        }

        if (party != null && partySize != queue.getTeamSize() && !queue.isHybridMode()) {
            return QueueResult.PARTY_SIZE_MISMATCH;
        }

        if (!hasQueuePermission(playerId, queue)) {
            return QueueResult.NO_PERMISSION;
        }

        PlayerData playerData = core.getDatabaseManager().loadPlayer(playerId);
        int elo = playerData != null ? playerData.getElo() : core.getEloManager().getInitialElo();

        MatchPlayer mp = bridge.player(playerId).orElse(null);
        boolean isVip = mp != null && mp.hasPermission("match.vip");

        QueueEntry entry = new QueueEntry(playerId, playerName, partyLeaderId, partySize, elo, isVip);
        queue.getEntries().add(entry);

        if (party != null) {
            for (UUID member : party.getMembers()) {
                playerQueueMap.put(member, queueId);
            }
            queue.addPlayer(partySize);
        } else {
            playerQueueMap.put(playerId, queueId);
            queue.addPlayer(1);
        }

        joinCooldown.put(playerId, System.currentTimeMillis());
        updatePositions();

        core.getMatchManager().startMatchTimer(playerId);

        if (mp != null) {
            core.getMessageManager().sendTitle(mp, "&e&l正在匹配...", "&7请稍候，正在寻找合适的房间");
        }

        core.eventBus().publish(new PlayerQueueJoinEvent(playerId, playerName, queueId));
        publishCross(Protocol.ACTION_QUEUE_JOIN, playerId + "," + queueId);

        return QueueResult.SUCCESS;
    }

    public boolean leaveQueue(UUID playerId) {
        String queueId = playerQueueMap.remove(playerId);
        if (queueId == null) {
            return false;
        }

        Queue queue = queues.get(queueId);
        if (queue == null) {
            return false;
        }

        Party party = core.getPartyManager().getParty(playerId);

        if (party != null && party.isLeader(playerId)) {
            for (UUID member : party.getMembers()) {
                removeFromQueue(member, queue);
                playerQueueMap.remove(member);
            }
            queue.removePlayer(party.getSize());
        } else {
            removeFromQueue(playerId, queue);
            queue.removePlayer(1);
        }

        updatePositions();
        core.eventBus().publish(new PlayerQueueLeaveEvent(playerId, queueId));
        publishCross(Protocol.ACTION_QUEUE_LEAVE, playerId.toString());
        return true;
    }

    private void removeFromQueue(UUID playerId, Queue queue) {
        queue.getEntries().removeIf(entry -> entry.getPlayerId().equals(playerId));
    }

    public Queue getQueue(String queueId) {
        return queues.get(queueId);
    }

    public Collection<Queue> getQueues() {
        return queues.values();
    }

    public Collection<Queue> getEnabledQueues() {
        return queues.values().stream().filter(Queue::isEnabled).collect(Collectors.toList());
    }

    public String getPlayerQueue(UUID playerId) {
        return playerQueueMap.get(playerId);
    }

    public boolean isInQueue(UUID playerId) {
        return playerQueueMap.containsKey(playerId);
    }

    public int getQueuePosition(UUID playerId) {
        String queueId = playerQueueMap.get(playerId);
        if (queueId == null) {
            return -1;
        }

        Queue queue = queues.get(queueId);
        if (queue == null) {
            return -1;
        }

        QueueEntry entry = queue.getEntries().stream()
            .filter(e -> e.getPlayerId().equals(playerId))
            .findFirst()
            .orElse(null);

        return entry != null ? entry.getPosition() : -1;
    }

    private boolean isOnCooldown(UUID playerId) {
        Long lastJoin = joinCooldown.get(playerId);
        if (lastJoin == null) {
            return false;
        }

        int cooldownSeconds = configManager.getInt(configManager.getConfig(), "matchmaking.join-cooldown", 5);
        return System.currentTimeMillis() - lastJoin < cooldownSeconds * 1000L;
    }

    private void updatePositions() {
        for (Queue queue : queues.values()) {
            List<QueueEntry> entries = new ArrayList<>(queue.getEntries());
            Collections.sort(entries);

            int position = 1;
            for (QueueEntry entry : entries) {
                entry.setPosition(position);
                position += entry.getPartySize();
            }
        }
    }

    public void createQueue(Queue queue) {
        queues.put(queue.getId(), queue);
    }

    public void deleteQueue(String queueId) {
        queues.remove(queueId);
    }

    /**
     * 重新从配置加载队列（供热重载）。
     */
    public void reload() {
        queues.clear();
        playerQueueMap.clear();
        joinCooldown.clear();
        loadQueues();
    }

    /**
     * 检查玩家是否有权限加入队列。普通队列默认允许；特殊队列需 match.spqueue.&lt;queueId&gt;。
     */
    public boolean hasQueuePermission(UUID playerId, Queue queue) {
        if (!queue.isSpecial()) {
            return true;
        }

        MatchPlayer player = bridge.player(playerId).orElse(null);
        if (player == null) {
            return false;
        }

        String permission = queue.getJoinPermission();
        boolean hasPermission = player.hasPermission(permission);

        if (!hasPermission) {
            core.logger().info("Player " + player.getName() + " lacks permission " + permission + " for queue " + queue.getId());
        }

        return hasPermission;
    }

    private void publishCross(String action, String data) {
        RedisManager redis = core.getRedisManager();
        if (redis != null) {
            redis.publish(action, data);
        }
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        queues.clear();
        playerQueueMap.clear();
        joinCooldown.clear();
    }
}
