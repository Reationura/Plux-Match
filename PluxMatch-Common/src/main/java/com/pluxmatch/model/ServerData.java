package com.pluxmatch.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 后端服务器状态模型（平台无关）。
 *
 * @author Reationura
 */
public class ServerData {

    private final String name;
    private final String group;
    private boolean enabled;
    private volatile int maxPlayers;
    private final int minPlayersToStart;

    private final AtomicInteger currentPlayers;
    private final AtomicInteger failedAttempts;
    private final AtomicLong lastPingTime;
    private boolean online;
    private String gameState;

    public ServerData(String name, String group, boolean enabled, int maxPlayers, int minPlayersToStart) {
        this.name = name;
        this.group = group;
        this.enabled = enabled;
        this.maxPlayers = maxPlayers;
        this.minPlayersToStart = minPlayersToStart;
        this.currentPlayers = new AtomicInteger(0);
        this.failedAttempts = new AtomicInteger(0);
        this.lastPingTime = new AtomicLong(0);
        this.online = false;
        this.gameState = "WAITING";
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * 依据服务器 ping 结果回写真实最大人数（仅当有效时覆盖配置值）。
     */
    public void setMaxPlayersFromPing(int max) {
        if (max > 0) {
            this.maxPlayers = max;
        }
    }

    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public int getCurrentPlayers() {
        return currentPlayers.get();
    }

    public void setCurrentPlayers(int count) {
        currentPlayers.set(count);
    }

    public void addPlayer() {
        currentPlayers.incrementAndGet();
    }

    public void removePlayer() {
        currentPlayers.decrementAndGet();
    }

    public int getFailedAttempts() {
        return failedAttempts.get();
    }

    public void incrementFailedAttempts() {
        failedAttempts.incrementAndGet();
    }

    public void resetFailedAttempts() {
        failedAttempts.set(0);
    }

    public long getLastPingTime() {
        return lastPingTime.get();
    }

    public void setLastPingTime(long time) {
        lastPingTime.set(time);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public boolean canJoin() {
        return online && enabled && currentPlayers.get() < maxPlayers && !"PLAYING".equals(gameState);
    }

    public boolean hasEnoughPlayers() {
        return currentPlayers.get() >= minPlayersToStart;
    }

    public double getLoadPercentage() {
        return (double) currentPlayers.get() / maxPlayers * 100;
    }
}
