package com.pluxmatch;

import com.pluxmatch.api.PluxMatchAPI;
import com.pluxmatch.command.CommandService;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.database.DatabaseManager;
import com.pluxmatch.elo.ELOManager;
import com.pluxmatch.event.CrossProxyMessage;
import com.pluxmatch.event.EventBus;
import com.pluxmatch.event.TierPromotionEvent;
import com.pluxmatch.guis.GUIManager;
import com.pluxmatch.match.MatchManager;
import com.pluxmatch.messaging.MessageHandler;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.party.PartyManager;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.queue.QueueManager;
import com.pluxmatch.redis.Protocol;
import com.pluxmatch.redis.RedisManager;
import com.pluxmatch.server.ServerManager;
import com.pluxmatch.tier.TierManager;
import com.pluxmatch.util.MessageManager;
import com.pluxmatch.util.PlaceholderService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * 平台无关的核心装配类：持有唯一 {@link PlatformBridge}，按固定顺序构建各管理器，
 * 自带线程池（不依赖平台调度器），并暴露 getter、玩家生命周期入口与热重载。
 * <p>
 * BungeeCord / Velocity 适配层只需构造 {@code new PluxMatchCore(bridge, version)}。
 *
 * @author Reationura
 */
public class PluxMatchCore {

    private final PlatformBridge bridge;
    private final String version;
    private final ExecutorService executorService;
    private final EventBus eventBus;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private ELOManager eloManager;
    private TierManager tierManager;
    private QueueManager queueManager;
    private PartyManager partyManager;
    private ServerManager serverManager;
    private MatchManager matchManager;
    private GUIManager guiManager;
    private CommandService commandService;
    private PlaceholderService placeholderService;
    private MessageHandler messageHandler;
    private RedisManager redisManager;
    private PluxMatchAPI api;

    public PluxMatchCore(PlatformBridge bridge, String version) {
        this.bridge = bridge;
        this.version = version;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "PluxMatch-Worker");
            t.setDaemon(true);
            return t;
        });
        this.eventBus = new EventBus();

        registerChannels();
        initManagers();
        registerEventHandlers();
    }

    private void registerChannels() {
        bridge.registerChannel(MessageHandler.DATA_CHANNEL);
        bridge.registerChannel(MessageHandler.COMMAND_CHANNEL);
        bridge.registerChannel(MessageHandler.RESPONSE_CHANNEL);
    }

    private void initManagers() {
        configManager = new ConfigManager(bridge);
        messageManager = new MessageManager(bridge, configManager);
        databaseManager = new DatabaseManager(this);
        eloManager = new ELOManager(this);
        tierManager = new TierManager(this);

        queueManager = new QueueManager(this);
        partyManager = new PartyManager(this);
        serverManager = new ServerManager(this);
        matchManager = new MatchManager(this);
        guiManager = new GUIManager(this);
        commandService = new CommandService(this);
        placeholderService = new PlaceholderService(this);
        messageHandler = new MessageHandler(this);
        api = new PluxMatchAPI(this);

        Map<String, Object> config = configManager.getConfig();
        if (configManager.getBoolean(config, "redis.enabled", false)) {
            redisManager = new RedisManager(this);
        }
    }

    private void registerEventHandlers() {
        // 升/降段时向玩家推送 Title 提示
        eventBus.subscribe(TierPromotionEvent.class, event ->
            bridge.player(event.getPlayerId()).ifPresent(player -> {
                String newTier = tierManager.getDisplayName(event.getNewTier());
                messageManager.sendTitle(player, "&6&l段位更新", "&f你现在是 " + newTier + "&f 段位");
                messageManager.sendMessage(player, "tier.promote", com.pluxmatch.util.MapUtils.of(
                    "old", String.valueOf(event.getOldTier()), "new", newTier));
            }));

        // 跨代理事件同步：Redis 收到的消息经事件总线在此落地真正的本地动作
        eventBus.subscribe(CrossProxyMessage.class, this::handleCrossProxy);
    }

    private void handleCrossProxy(CrossProxyMessage message) {
        switch (message.getAction()) {
            case Protocol.ACTION_QUEUE_LEAVE: {
                // 其他网关上该玩家已退队/离线，若本代理仍保留其队列成员则幂等清理
                UUID playerId = parseUuid(message.getData());
                if (playerId != null && queueManager.isInQueue(playerId)) {
                    queueManager.leaveQueue(playerId);
                }
                break;
            }
            case Protocol.ACTION_QUEUE_JOIN: {
                logger().fine("[cross-proxy] queue join: " + message.getData());
                break;
            }
            case Protocol.ACTION_MATCH_START:
            case Protocol.ACTION_MATCH_END: {
                logger().fine("[cross-proxy] " + message.getAction() + ": " + message.getData());
                break;
            }
            default:
                break;
        }
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String token = raw.split(",")[0].trim();
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* ---------- 玩家生命周期（供适配层监听器转调） ---------- */

    public void handlePlayerJoin(MatchPlayer player) {
        UUID id = player.getUniqueId();
        PlayerData data = databaseManager.loadPlayer(id);
        if (data == null) {
            data = new PlayerData(id, player.getName());
            databaseManager.savePlayer(data);
        }
        logger().info("Player joined: " + player.getName());
    }

    public void handlePlayerQuit(MatchPlayer player) {
        UUID id = player.getUniqueId();
        PlayerData data = databaseManager.loadPlayer(id);
        if (data != null) {
            data.setLastLeaveTime(System.currentTimeMillis());
            databaseManager.savePlayer(data);
        }
        queueManager.leaveQueue(id);
        partyManager.leaveParty(id);
        logger().info("Player quit: " + player.getName());
    }

    public void handleServerPostConnect(MatchPlayer player) {
        matchManager.confirmTransferArrival(player.getUniqueId());
    }

    /* ---------- 热重载（§6 handleReload 修复） ---------- */

    public void reload() {
        configManager.reload();
        queueManager.reload();
        serverManager.reload();
        tierManager.reload();
        matchManager.reload();
        logger().info("PluxMatch configuration reloaded");
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (queueManager != null) {
            queueManager.shutdown();
        }
        if (matchManager != null) {
            matchManager.shutdown();
        }
        if (serverManager != null) {
            serverManager.shutdown();
        }
        if (redisManager != null) {
            redisManager.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        eventBus.clear();
    }

    /* ---------- 访问器 ---------- */

    public PlatformBridge bridge() {
        return bridge;
    }

    public String version() {
        return version;
    }

    public ExecutorService executor() {
        return executorService;
    }

    public Logger logger() {
        return bridge.logger();
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public PluxMatchAPI api() {
        return api;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ELOManager getEloManager() {
        return eloManager;
    }

    public TierManager getTierManager() {
        return tierManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public CommandService getCommandService() {
        return commandService;
    }

    public PlaceholderService getPlaceholderService() {
        return placeholderService;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }
}
