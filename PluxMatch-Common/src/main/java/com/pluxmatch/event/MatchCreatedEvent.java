package com.pluxmatch.event;

import java.util.UUID;

/**
 * 匹配创建（玩家被分配到房间）事件。
 *
 * @author Reationura
 */
public class MatchCreatedEvent {
    private final String matchId;
    private final UUID playerId;
    private final String queueId;
    private final String serverName;

    public MatchCreatedEvent(String matchId, UUID playerId, String queueId, String serverName) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.queueId = queueId;
        this.serverName = serverName;
    }

    public String getMatchId() {
        return matchId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getQueueId() {
        return queueId;
    }

    public String getServerName() {
        return serverName;
    }
}
