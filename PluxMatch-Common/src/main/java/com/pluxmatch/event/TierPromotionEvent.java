package com.pluxmatch.event;

import java.util.UUID;

/**
 * 玩家段位变更（升/降段）事件。
 *
 * @author Reationura
 */
public class TierPromotionEvent {
    private final UUID playerId;
    private final String oldTier;
    private final String newTier;

    public TierPromotionEvent(UUID playerId, String oldTier, String newTier) {
        this.playerId = playerId;
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getOldTier() {
        return oldTier;
    }

    public String getNewTier() {
        return newTier;
    }
}
