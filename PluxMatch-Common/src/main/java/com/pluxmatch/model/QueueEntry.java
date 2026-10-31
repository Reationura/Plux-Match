package com.pluxmatch.model;

import java.util.UUID;

/**
 * 队列条目（平台无关）。
 *
 * @author Reationura
 */
public class QueueEntry implements Comparable<QueueEntry> {

    private final UUID playerId;
    private final String playerName;
    private final UUID partyLeaderId;
    private final int partySize;
    private final int elo;
    private final long joinTime;
    private final boolean isVip;
    private int position;

    public QueueEntry(UUID playerId, String playerName, UUID partyLeaderId, int partySize, int elo, boolean isVip) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.partyLeaderId = partyLeaderId;
        this.partySize = partySize;
        this.elo = elo;
        this.joinTime = System.currentTimeMillis();
        this.isVip = isVip;
        this.position = 0;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPartyLeaderId() {
        return partyLeaderId;
    }

    public boolean isPartyLeader() {
        return partyLeaderId == null || partyLeaderId.equals(playerId);
    }

    public int getPartySize() {
        return partySize;
    }

    public boolean isInParty() {
        return partySize > 1;
    }

    public int getElo() {
        return elo;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public long getWaitTime() {
        return System.currentTimeMillis() - joinTime;
    }

    public boolean isVip() {
        return isVip;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int compareTo(QueueEntry other) {
        if (isVip != other.isVip) {
            return isVip ? -1 : 1;
        }
        return Long.compare(joinTime, other.joinTime);
    }
}
