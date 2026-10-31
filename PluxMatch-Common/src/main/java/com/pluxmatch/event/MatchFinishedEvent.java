package com.pluxmatch.event;

import java.util.List;
import java.util.UUID;

/**
 * 对局结束结算事件。
 *
 * @author Reationura
 */
public class MatchFinishedEvent {
    private final String matchId;
    private final String queueId;
    private final List<UUID> winners;
    private final List<UUID> losers;

    public MatchFinishedEvent(String matchId, String queueId, List<UUID> winners, List<UUID> losers) {
        this.matchId = matchId;
        this.queueId = queueId;
        this.winners = winners;
        this.losers = losers;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getQueueId() {
        return queueId;
    }

    public List<UUID> getWinners() {
        return winners;
    }

    public List<UUID> getLosers() {
        return losers;
    }
}
