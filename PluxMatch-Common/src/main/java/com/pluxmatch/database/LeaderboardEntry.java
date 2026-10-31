package com.pluxmatch.database;

/**
 * 排行榜条目。
 *
 * @author Reationura
 */
public class LeaderboardEntry {

    public final String uuid;
    public final String name;
    public final int elo;
    public final int wins;
    public final int losses;

    public LeaderboardEntry(String uuid, String name, int elo, int wins, int losses) {
        this.uuid = uuid;
        this.name = name;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }

    public double winRate() {
        int total = wins + losses;
        return total == 0 ? 0 : (double) wins / total * 100;
    }
}
