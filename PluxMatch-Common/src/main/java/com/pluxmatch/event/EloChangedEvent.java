package com.pluxmatch.event;

import java.util.UUID;

/**
 * 玩家 ELO 变动事件。
 *
 * @author Reationura
 */
public class EloChangedEvent {
    private final UUID playerId;
    private final int oldElo;
    private final int newElo;

    public EloChangedEvent(UUID playerId, int oldElo, int newElo) {
        this.playerId = playerId;
        this.oldElo = oldElo;
        this.newElo = newElo;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getOldElo() {
        return oldElo;
    }

    public int getNewElo() {
        return newElo;
    }

    public int getChange() {
        return newElo - oldElo;
    }
}
