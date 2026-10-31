package com.pluxmatch.event;

import java.util.UUID;

/**
 * 玩家退出匹配队列事件。
 *
 * @author Reationura
 */
public class PlayerQueueLeaveEvent {
    private final UUID playerId;
    private final String queueId;

    public PlayerQueueLeaveEvent(UUID playerId, String queueId) {
        this.playerId = playerId;
        this.queueId = queueId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getQueueId() {
        return queueId;
    }
}
