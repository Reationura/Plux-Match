package com.pluxmatch.match;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.elo.ELOManager;
import com.pluxmatch.event.EloChangedEvent;
import com.pluxmatch.event.MatchCreatedEvent;
import com.pluxmatch.event.MatchFinishedEvent;
import com.pluxmatch.model.MatchRecord;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.model.Queue;
import com.pluxmatch.model.QueueEntry;
import com.pluxmatch.model.ServerData;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.queue.QueueManager;
import com.pluxmatch.redis.Protocol;
import com.pluxmatch.redis.RedisManager;
import com.pluxmatch.server.ServerManager;
import com.pluxmatch.tier.TierManager;
import com.pluxmatch.util.ByteBufWriter;
import com.pluxmatch.util.MapUtils;
import com.pluxmatch.util.MessageManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 匹配管理（平台无关）：按“每人独立找房”模型推进匹配、创建对局、校验传送、结算 ELO/段位。
 * <p>
 * 相比旧版修复：
 * <ul>
 *   <li>{@code checkTransferTimeouts} 去除硬编码 {@code contains("lobby")}，改为记录目标服务器名，
 *       用 {@link MatchPlayer#currentServerName()} 判定传送是否失败；</li>
 *   <li>{@code finishMatch} 统一先加载真实 ELO，缺失才回退初始分；结算后刷新段位并发布事件；</li>
 *   <li>Plugin Message 编码不再依赖 Guava。</li>
 * </ul>
 *
 * @author Reationura
 */
public class MatchManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final PlatformBridge bridge;
    private final QueueManager queueManager;
    private final ServerManager serverManager;
    private final ELOManager eloManager;
    private final TierManager tierManager;
    private final MessageManager messages;

    private final ConcurrentHashMap<String, MatchRecord> activeMatches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> matchStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TransferInfo> pendingTransfers = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<MatchFilter> filters = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService matchExecutor;

    private static final int MATCH_TIMEOUT_SECONDS = 3;
    private static final int TRANSFER_TIMEOUT_SECONDS = 5;
    private static final String RESPONSE_CHANNEL = "pluxmatch:response";

    private String timeoutStrategy;
    private int eloBandInitial;
    private int eloBandMax;
    private int eloBandPerSecond;

    public MatchManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        this.bridge = core.bridge();
        this.queueManager = core.getQueueManager();
        this.serverManager = core.getServerManager();
        this.eloManager = core.getEloManager();
        this.tierManager = core.getTierManager();
        this.messages = core.getMessageManager();

        loadSettings();
        startMatchScheduler();
    }

    private void loadSettings() {
        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> mm = configManager.getSection(config, "matchmaking");
        this.timeoutStrategy = configManager.getString(mm, "timeout-strategy", "expand_elo");
        Map<String, Object> band = configManager.getSection(mm, "elo-band");
        this.eloBandInitial = configManager.getInt(band, "initial", 100);
        this.eloBandMax = configManager.getInt(band, "max", 500);
        this.eloBandPerSecond = configManager.getInt(band, "per-second", 25);
    }

    /**
     * 供热重载重新读取匹配相关配置。
     */
    public void reload() {
        loadSettings();
    }

    private void startMatchScheduler() {
        matchExecutor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "PluxMatch-Match-Scheduler");
            t.setDaemon(true);
            return t;
        });
        matchExecutor.scheduleAtFixedRate(this::processMatches, 0, 500, TimeUnit.MILLISECONDS);
        matchExecutor.scheduleAtFixedRate(this::checkTransferTimeouts, 1, 1, TimeUnit.SECONDS);
    }

    public void processMatches() {
        long now = System.currentTimeMillis();

        for (Queue queue : queueManager.getEnabledQueues()) {
            if (!queue.isEnabled()) {
                continue;
            }

            for (QueueEntry entry : new ArrayList<>(queue.getEntries())) {
                UUID playerId = entry.getPlayerId();

                Long startTime = matchStartTimes.get(playerId);
                if (startTime != null && (now - startTime) > MATCH_TIMEOUT_SECONDS * 1000L) {
                    handleMatchTimeout(playerId, queue);
                    continue;
                }

                tryMatchPlayer(entry, queue);
            }
        }
    }

    private void tryMatchPlayer(QueueEntry entry, Queue queue) {
        UUID playerId = entry.getPlayerId();
        MatchPlayer player = bridge.player(playerId).orElse(null);

        if (player == null) {
            queueManager.leaveQueue(playerId);
            matchStartTimes.remove(playerId);
            return;
        }

        ServerData server = serverManager.findBestServer(
            queue.getServerGroup(), queue.getPopulationMode(), bandAndFilterPredicate(entry));

        if (server == null) {
            return;
        }

        createMatch(queue, player, server);
    }

    /**
     * 组合 ELO 分段带宽（{@code expand_elo}）与第三方 {@link MatchFilter}，共同作为选服额外过滤条件。
     * 等待时间越长，容差 {@code eloBand} 越大，可接受的目标房间负载上限随之放宽，从而提升匹配命中率。
     */
    private Predicate<ServerData> bandAndFilterPredicate(QueueEntry entry) {
        long waitedSeconds = Math.max(0, (System.currentTimeMillis() - entry.getJoinTime()) / 1000L);
        int tolerance = currentEloBand(waitedSeconds);

        return server -> {
            for (MatchFilter filter : filters) {
                if (!filter.test(entry, server)) {
                    return false;
                }
            }
            if (!"expand_elo".equalsIgnoreCase(timeoutStrategy)) {
                return true;
            }
            // 容差越大越宽松：早期只进几乎空的新房，随等待放宽到较满的房间。
            int loadCap = (int) Math.round(server.getMaxPlayers() * (tolerance / (double) eloBandMax));
            return server.getCurrentPlayers() <= Math.max(0, loadCap);
        };
    }

    public int currentEloBand(long waitedSeconds) {
        long band = eloBandInitial + waitedSeconds * eloBandPerSecond;
        return (int) Math.min(eloBandMax, band);
    }

    private void handleMatchTimeout(UUID playerId, Queue queue) {
        MatchPlayer player = bridge.player(playerId).orElse(null);

        String queueId = queue.getId();
        queueManager.leaveQueue(playerId);
        matchStartTimes.remove(playerId);

        if (player != null) {
            sendMatchFailed(player, "&c&l匹配失败", "&7超时未找到房间", "&c匹配超时，请稍后重试");
            core.logger().info("Match timeout for player: " + player.getName() + " in queue: " + queueId);
        }
    }

    private void createMatch(Queue queue, MatchPlayer player, ServerData server) {
        String matchId = UUID.randomUUID().toString();

        queueManager.leaveQueue(player.getUniqueId());
        matchStartTimes.remove(player.getUniqueId());

        MatchRecord record = new MatchRecord(matchId, queue.getId(), server.getName());
        activeMatches.put(matchId, record);

        sendMatchSuccess(player, "&a&l匹配成功！", "&7正在传送至 " + server.getName(), "&a你已成功匹配到游戏房间！");

        pendingTransfers.put(player.getUniqueId(),
            new TransferInfo(System.currentTimeMillis(), server.getName(), queue.getId()));

        serverManager.teleportPlayer(player, server.getName());

        queue.incrementMatchCount();
        core.eventBus().publish(new MatchCreatedEvent(matchId, player.getUniqueId(), queue.getId(), server.getName()));
        publishCross(Protocol.ACTION_MATCH_START, matchId + "," + player.getUniqueId() + "," + server.getName());
        core.logger().info("Created match " + matchId + " for player " + player.getName() + " on " + server.getName());
    }

    private void checkTransferTimeouts() {
        long now = System.currentTimeMillis();
        List<UUID> timedOut = new ArrayList<>();

        for (Map.Entry<UUID, TransferInfo> e : pendingTransfers.entrySet()) {
            if (now - e.getValue().time > TRANSFER_TIMEOUT_SECONDS * 1000L) {
                timedOut.add(e.getKey());
            }
        }

        for (UUID playerId : timedOut) {
            TransferInfo info = pendingTransfers.remove(playerId);
            MatchPlayer player = bridge.player(playerId).orElse(null);
            if (player == null || info == null) {
                continue;
            }

            // §6 修复：不再用 "lobby" 硬编码，直接比较当前所在服务器与目标服务器。
            String current = player.currentServerName();
            boolean transferFailed = current == null || current.equals(info.targetServer);

            if (transferFailed) {
                sendMatchFailed(player, "&c&l传送超时", "&7请稍后重试", "&c传送超时，正在重新匹配...");
                core.logger().warning("Transfer timeout for player: " + player.getName());

                String queueId = info.queueId != null ? info.queueId : "solo-bedwars";
                queueManager.joinQueue(playerId, player.getName(), queueId);
            }
        }
    }

    public void startMatchTimer(UUID playerId) {
        matchStartTimes.put(playerId, System.currentTimeMillis());
    }

    public void confirmTransferArrival(UUID playerId) {
        pendingTransfers.remove(playerId);
    }

    private void sendMatchSuccess(MatchPlayer player, String title, String subtitle, String message) {
        if (player.currentServerName() == null) {
            return;
        }
        byte[] payload = ByteBufWriter.create()
            .utf("MatchSuccess").utf(player.getName()).utf(title).utf(subtitle).utf(message)
            .toByteArray();
        bridge.sendData(player, RESPONSE_CHANNEL, payload);
    }

    private void sendMatchFailed(MatchPlayer player, String title, String subtitle, String message) {
        if (player.currentServerName() == null) {
            return;
        }
        byte[] payload = ByteBufWriter.create()
            .utf("MatchFailed").utf(player.getName()).utf(title).utf(subtitle).utf(message)
            .toByteArray();
        bridge.sendData(player, RESPONSE_CHANNEL, payload);
    }

    public void finishMatch(String matchId, List<UUID> winners, List<UUID> losers) {
        MatchRecord record = activeMatches.remove(matchId);
        if (record == null) {
            return;
        }

        // §6 修复：统一先取真实 ELO，缺失才回退初始分。
        Map<UUID, Integer> allPlayers = new HashMap<>();
        for (UUID id : winners) {
            allPlayers.put(id, resolveElo(id));
        }
        for (UUID id : losers) {
            allPlayers.put(id, resolveElo(id));
        }

        Map<UUID, Integer> team1 = winners.stream()
            .filter(allPlayers::containsKey)
            .collect(Collectors.toMap(id -> id, allPlayers::get));
        Map<UUID, Integer> team2 = losers.stream()
            .filter(allPlayers::containsKey)
            .collect(Collectors.toMap(id -> id, allPlayers::get));

        Map<UUID, Integer> newElos = eloManager.calculateMatchElos(team1, team2, true);

        for (Map.Entry<UUID, Integer> entry : newElos.entrySet()) {
            UUID playerId = entry.getKey();
            int newElo = entry.getValue();

            PlayerData playerData = core.getDatabaseManager().loadPlayer(playerId);
            if (playerData == null) {
                continue;
            }

            int oldElo = playerData.getElo();
            int eloChange = newElo - oldElo;
            boolean won = winners.contains(playerId);

            if (won) {
                playerData.setWins(playerData.getWins() + 1);
            } else {
                playerData.setLosses(playerData.getLosses() + 1);
            }
            playerData.setElo(newElo);
            tierManager.updatePlayerTier(playerData);
            core.getDatabaseManager().savePlayer(playerData);

            record.addPlayerResult(playerId, won, eloChange);
            core.eventBus().publish(new EloChangedEvent(playerId, oldElo, newElo));

            bridge.player(playerId).ifPresent(p ->
                messages.sendMessage(p, "stats.elo", MapUtils.of("elo", String.valueOf(newElo))));
        }

        record.setResult(MatchRecord.MatchResult.COMPLETED);
        record.setEndTime(System.currentTimeMillis());
        core.getDatabaseManager().saveMatchRecord(record);

        core.eventBus().publish(new MatchFinishedEvent(matchId, record.getQueueId(), winners, losers));
        publishCross(Protocol.ACTION_MATCH_END, matchId);
    }

    private int resolveElo(UUID playerId) {
        PlayerData data = core.getDatabaseManager().loadPlayer(playerId);
        return data != null ? data.getElo() : eloManager.getInitialElo();
    }

    public void registerMatchFilter(MatchFilter filter) {
        if (filter != null) {
            filters.add(filter);
        }
    }

    public List<MatchFilter> getMatchFilters() {
        return new ArrayList<>(filters);
    }

    public MatchRecord getActiveMatch(String matchId) {
        return activeMatches.get(matchId);
    }

    public Collection<MatchRecord> getActiveMatches() {
        return activeMatches.values();
    }

    private void publishCross(String action, String data) {
        RedisManager redis = core.getRedisManager();
        if (redis != null) {
            redis.publish(action, data);
        }
    }

    public void shutdown() {
        if (matchExecutor != null) {
            matchExecutor.shutdown();
        }
        activeMatches.clear();
        matchStartTimes.clear();
        pendingTransfers.clear();
    }

    /**
     * 传送验证信息，记录目标服务器与来源队列，供 §6 传送失败判定与重新入队使用。
     */
    private static final class TransferInfo {
        final long time;
        final String targetServer;
        final String queueId;

        TransferInfo(long time, String targetServer, String queueId) {
            this.time = time;
            this.targetServer = targetServer;
            this.queueId = queueId;
        }
    }
}
