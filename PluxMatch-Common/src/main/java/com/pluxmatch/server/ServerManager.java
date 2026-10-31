package com.pluxmatch.server;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.model.Queue;
import com.pluxmatch.model.ServerData;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 后端服务器管理（平台无关）：探活、故障剔除、按人口策略选服、传送。
 *
 * @author Reationura
 */
public class ServerManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final PlatformBridge bridge;

    private final ConcurrentHashMap<String, ServerData> servers;
    private final ConcurrentHashMap<String, List<String>> serverGroups;

    private ScheduledExecutorService pingExecutor;
    private int maxFailures;

    public ServerManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        this.bridge = core.bridge();

        this.servers = new ConcurrentHashMap<>();
        this.serverGroups = new ConcurrentHashMap<>();

        Map<String, Object> config = configManager.getConfig();
        this.maxFailures = configManager.getInt(config, "server.max-failures", 3);

        loadServers();
        startPingScheduler();
    }

    private void loadServers() {
        Map<String, Object> serversConfig = configManager.getServers();

        Map<String, Object> groupsSection = configManager.getSection(serversConfig, "server-groups");
        for (Map.Entry<String, Object> entry : groupsSection.entrySet()) {
            String groupName = entry.getKey();
            Map<String, Object> groupData = configManager.getSection(serversConfig, "server-groups." + groupName);

            List<String> serverNames = new ArrayList<>();
            Object serversList = groupData.get("servers");
            if (serversList instanceof List) {
                for (Object obj : (List<?>) serversList) {
                    serverNames.add(obj.toString());
                }
            }
            serverGroups.put(groupName, serverNames);
        }

        Map<String, Object> serversSection = configManager.getSection(serversConfig, "servers");
        for (Map.Entry<String, Object> entry : serversSection.entrySet()) {
            String serverName = entry.getKey();
            Map<String, Object> serverData = configManager.getSection(serversConfig, "servers." + serverName);

            ServerData data = new ServerData(
                serverName,
                configManager.getString(serverData, "group", "default"),
                configManager.getBoolean(serverData, "enabled", true),
                configManager.getInt(serverData, "max-players", 20),
                configManager.getInt(serverData, "min-players-to-start", 2)
            );

            servers.put(serverName, data);
            core.logger().info("Loaded server: " + serverName);
        }
    }

    private void startPingScheduler() {
        pingExecutor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r, "PluxMatch-Server-Ping");
            t.setDaemon(true);
            return t;
        });

        int interval = configManager.getInt(configManager.getConfig(), "server.ping-interval", 5);
        pingExecutor.scheduleAtFixedRate(this::pingAllServers, 0, interval, TimeUnit.SECONDS);
    }

    private void pingAllServers() {
        for (ServerData server : servers.values()) {
            if (!server.isEnabled()) {
                continue;
            }

            if (!bridge.hasRegisteredServer(server.getName())) {
                server.setOnline(false);
                server.incrementFailedAttempts();
                continue;
            }

            bridge.pingServer(server.getName(), (online, players, maxPlayers, versionName) -> {
                if (!online) {
                    server.setOnline(false);
                    server.incrementFailedAttempts();
                    if (server.getFailedAttempts() >= maxFailures) {
                        server.setEnabled(false);
                        core.logger().warning("Server " + server.getName() + " disabled due to failures");
                    }
                } else {
                    server.setOnline(true);
                    server.resetFailedAttempts();
                    server.setCurrentPlayers(players);
                    if (maxPlayers > 0) {
                        server.setMaxPlayersFromPing(maxPlayers);
                    }
                    if (versionName != null) {
                        server.setGameState(versionName);
                    }
                }
                server.setLastPingTime(System.currentTimeMillis());
            });
        }
    }

    public ServerData getServer(String name) {
        return servers.get(name);
    }

    public Collection<ServerData> getServers() {
        return servers.values();
    }

    public Collection<ServerData> getServersInGroup(String groupName) {
        List<String> serverNames = serverGroups.get(groupName);
        if (serverNames == null) {
            return Collections.emptyList();
        }

        List<ServerData> result = new ArrayList<>();
        for (String name : serverNames) {
            ServerData server = servers.get(name);
            if (server != null) {
                result.add(server);
            }
        }
        return result;
    }

    public ServerData findBestServer(String groupName, Queue.PopulationMode mode) {
        return findBestServer(groupName, mode, s -> true);
    }

    /**
     * 在指定组内按人口策略挑选一个可加入的服务器，并额外应用 {@code extraFilter}。
     */
    public ServerData findBestServer(String groupName, Queue.PopulationMode mode, Predicate<ServerData> extraFilter) {
        Collection<ServerData> availableServers = getServersInGroup(groupName).stream()
            .filter(ServerData::canJoin)
            .filter(extraFilter)
            .collect(Collectors.toList());

        if (availableServers.isEmpty()) {
            return null;
        }

        switch (mode) {
            case MOST:
                return availableServers.stream()
                    .max(Comparator.comparingInt(ServerData::getCurrentPlayers))
                    .orElse(null);

            case LEAST:
                return availableServers.stream()
                    .min(Comparator.comparingInt(ServerData::getCurrentPlayers))
                    .orElse(null);

            case RANDOM:
            default:
                List<ServerData> list = new ArrayList<>(availableServers);
                return list.get(new Random().nextInt(list.size()));
        }
    }

    public void teleportPlayer(MatchPlayer player, String serverName) {
        bridge.teleport(player, serverName);
    }

    public void teleportPlayers(List<MatchPlayer> players, String serverName) {
        for (MatchPlayer player : players) {
            bridge.teleport(player, serverName);
        }
    }

    public boolean addServerToGroup(String serverName, String groupName) {
        List<String> servers = serverGroups.computeIfAbsent(groupName, k -> new ArrayList<>());
        if (!servers.contains(serverName)) {
            servers.add(serverName);
            return true;
        }
        return false;
    }

    public boolean removeServerFromGroup(String serverName) {
        boolean removed = false;
        for (List<String> servers : serverGroups.values()) {
            removed |= servers.remove(serverName);
        }
        return removed;
    }

    /**
     * 获取指定组的房间列表，格式：服务器名|状态|玩家数|最大玩家数。
     */
    public List<String> getRoomsByGroup(String groupName) {
        List<String> rooms = new ArrayList<>();
        List<String> serverNames = serverGroups.get(groupName);

        if (serverNames == null || serverNames.isEmpty()) {
            return rooms;
        }

        for (String serverName : serverNames) {
            ServerData serverData = servers.get(serverName);
            if (serverData == null) {
                continue;
            }

            String status = serverData.isOnline() ? "ONLINE" : "OFFLINE";
            int playerCount = serverData.getCurrentPlayers();
            int maxPlayers = serverData.getMaxPlayers();

            rooms.add(serverName + "|" + status + "|" + playerCount + "|" + maxPlayers);
        }

        return rooms;
    }

    /**
     * 重新从配置加载服务器与分组（供热重载）。
     */
    public void reload() {
        servers.clear();
        serverGroups.clear();
        Map<String, Object> config = configManager.getConfig();
        this.maxFailures = configManager.getInt(config, "server.max-failures", 3);
        loadServers();
    }

    public void shutdown() {
        if (pingExecutor != null) {
            pingExecutor.shutdown();
        }
        servers.clear();
        serverGroups.clear();
    }
}
