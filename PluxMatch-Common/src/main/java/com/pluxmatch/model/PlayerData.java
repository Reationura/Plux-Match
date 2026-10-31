package com.pluxmatch.model;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据模型（平台无关），新增段位（tier）字段。
 *
 * @author Reationura
 */
public class PlayerData {

    private final UUID uuid;
    private final String name;
    private int elo;
    private int wins;
    private int losses;
    private long lastJoinTime;
    private long lastLeaveTime;
    private String currentQueue;
    private String currentServer;
    private String tier; // 当前段位名称，由 TierManager 依 ELO 计算

    private final ConcurrentHashMap<String, Integer> eloByQueue;
    private final ConcurrentHashMap<String, Integer> winsByQueue;
    private final ConcurrentHashMap<String, Integer> lossesByQueue;

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.elo = 1000;
        this.wins = 0;
        this.losses = 0;
        this.lastJoinTime = 0;
        this.lastLeaveTime = 0;
        this.currentQueue = null;
        this.currentServer = null;
        this.tier = null;
        this.eloByQueue = new ConcurrentHashMap<>();
        this.winsByQueue = new ConcurrentHashMap<>();
        this.lossesByQueue = new ConcurrentHashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0;
        return (double) wins / total * 100;
    }

    public int getTotalMatches() {
        return wins + losses;
    }

    public long getLastJoinTime() {
        return lastJoinTime;
    }

    public void setLastJoinTime(long lastJoinTime) {
        this.lastJoinTime = lastJoinTime;
    }

    public long getLastLeaveTime() {
        return lastLeaveTime;
    }

    public void setLastLeaveTime(long lastLeaveTime) {
        this.lastLeaveTime = lastLeaveTime;
    }

    public String getCurrentQueue() {
        return currentQueue;
    }

    public void setCurrentQueue(String currentQueue) {
        this.currentQueue = currentQueue;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public int getEloByQueue(String queueName) {
        return eloByQueue.getOrDefault(queueName, 1000);
    }

    public void setEloByQueue(String queueName, int elo) {
        eloByQueue.put(queueName, elo);
    }

    public int getWinsByQueue(String queueName) {
        return winsByQueue.getOrDefault(queueName, 0);
    }

    public void setWinsByQueue(String queueName, int wins) {
        winsByQueue.put(queueName, wins);
    }

    public int getLossesByQueue(String queueName) {
        return lossesByQueue.getOrDefault(queueName, 0);
    }

    public void setLossesByQueue(String queueName, int losses) {
        lossesByQueue.put(queueName, losses);
    }
}
