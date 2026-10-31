package com.pluxmatch.event;

import java.util.UUID;

/**
 * 玩家加入匹配队列事件。
 *
 * @author Reationura
 */
public class PlayerQueueJoinEvent {
    private final UUID playerId;
    private final String playerName;
    private final String queueId;

    public PlayerQueueJoinEvent(UUID playerId, String playerName, String queueId) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.queueId = queueId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getQueueId() {
        return queueId;
    }
}
