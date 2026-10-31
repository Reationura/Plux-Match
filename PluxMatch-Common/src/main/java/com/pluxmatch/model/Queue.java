package com.pluxmatch.model;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 匹配队列模型（平台无关）。
 *
 * @author Reationura
 */
public class Queue {

    private final String id;
    private final String name;
    private final String description;
    private boolean enabled;
    private final String group;
    private final int minPlayers;
    private final int maxPlayers;
    private final int teamSize;
    private AlgorithmType algorithm;
    private PopulationMode populationMode;
    private boolean allowParty;
    private boolean hybridMode;
    private int timeout;
    private final String joinPermission;
    private final String serverGroup;
    private final boolean special; // 特殊队列标记，需要特定权限

    private final ConcurrentLinkedQueue<QueueEntry> entries;
    private final AtomicInteger matchCount;
    private final AtomicInteger totalPlayers;

    public Queue(String id, String name, String description, boolean enabled, String group,
                 int minPlayers, int maxPlayers, int teamSize, AlgorithmType algorithm,
                 PopulationMode populationMode, boolean allowParty, boolean hybridMode,
                 int timeout, String joinPermission, String serverGroup, boolean special) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.group = group;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teamSize = teamSize;
        this.algorithm = algorithm;
        this.populationMode = populationMode;
        this.allowParty = allowParty;
        this.hybridMode = hybridMode;
        this.timeout = timeout;
        this.joinPermission = joinPermission;
        this.serverGroup = serverGroup;
        this.special = special;
        this.entries = new ConcurrentLinkedQueue<>();
        this.matchCount = new AtomicInteger(0);
        this.totalPlayers = new AtomicInteger(0);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroup() {
        return group;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public PopulationMode getPopulationMode() {
        return populationMode;
    }

    public void setPopulationMode(PopulationMode populationMode) {
        this.populationMode = populationMode;
    }

    public boolean isAllowParty() {
        return allowParty;
    }

    public void setAllowParty(boolean allowParty) {
        this.allowParty = allowParty;
    }

    public boolean isHybridMode() {
        return hybridMode;
    }

    public void setHybridMode(boolean hybridMode) {
        this.hybridMode = hybridMode;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getJoinPermission() {
        return joinPermission;
    }

    public String getServerGroup() {
        return serverGroup;
    }

    public boolean isSpecial() {
        return special;
    }

    public ConcurrentLinkedQueue<QueueEntry> getEntries() {
        return entries;
    }

    public int getMatchCount() {
        return matchCount.get();
    }

    public void incrementMatchCount() {
        matchCount.incrementAndGet();
    }

    public int getTotalPlayers() {
        return totalPlayers.get();
    }

    public void addPlayer(int count) {
        totalPlayers.addAndGet(count);
    }

    public void removePlayer(int count) {
        totalPlayers.addAndGet(-count);
    }

    public int getCurrentSize() {
        return entries.size();
    }

    public enum AlgorithmType {
        FIFO, POPULATION
    }

    public enum PopulationMode {
        MOST, LEAST, RANDOM
    }
}
