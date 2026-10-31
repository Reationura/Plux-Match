package com.pluxmatch.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对局记录模型（平台无关）。
 *
 * @author Reationura
 */
public class MatchRecord {

    private final String id;
    private final String queueId;
    private final long startTime;
    private long endTime;
    private final String serverName;
    private final List<UUID> winners;
    private final List<UUID> losers;
    private final List<PlayerResult> playerResults;
    private MatchResult result;

    public MatchRecord(String id, String queueId, String serverName) {
        this.id = id;
        this.queueId = queueId;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.serverName = serverName;
        this.winners = new ArrayList<>();
        this.losers = new ArrayList<>();
        this.playerResults = new ArrayList<>();
        this.result = MatchResult.IN_PROGRESS;
    }

    public String getId() {
        return id;
    }

    public String getQueueId() {
        return queueId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getServerName() {
        return serverName;
    }

    public List<UUID> getWinners() {
        return winners;
    }

    public List<UUID> getLosers() {
        return losers;
    }

    public List<PlayerResult> getPlayerResults() {
        return playerResults;
    }

    public MatchResult getResult() {
        return result;
    }

    public void setResult(MatchResult result) {
        this.result = result;
    }

    public void addWinner(UUID player) {
        winners.add(player);
    }

    public void addLoser(UUID player) {
        losers.add(player);
    }

    public void addPlayerResult(UUID player, boolean won, int eloChange) {
        playerResults.add(new PlayerResult(player, won, eloChange));
    }

    public int getDuration() {
        return (int) ((endTime > 0 ? endTime : System.currentTimeMillis()) - startTime) / 1000;
    }

    public static class PlayerResult {
        private final UUID playerId;
        private final boolean won;
        private final int eloChange;

        public PlayerResult(UUID playerId, boolean won, int eloChange) {
            this.playerId = playerId;
            this.won = won;
            this.eloChange = eloChange;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public boolean isWon() {
            return won;
        }

        public int getEloChange() {
            return eloChange;
        }
    }

    public enum MatchResult {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
