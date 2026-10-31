package com.pluxmatch.util;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.model.Party;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;

import java.util.UUID;

/**
 * 占位符数据逻辑（平台无关）。Bungee 侧由 PlaceholderAPI 扩展转调，
 * Velocity 侧可接入相应占位桥；新增 {@code pluxmatch_tier}、{@code pluxmatch_leaderboard_rank}。
 *
 * @author Reationura
 */
public class PlaceholderService {

    private final PluxMatchCore core;
    private final PlatformBridge bridge;

    public PlaceholderService(PluxMatchCore core) {
        this.core = core;
        this.bridge = core.bridge();
    }

    public String onRequest(UUID playerId, String identifier) {
        if (playerId == null || identifier == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "queue":
                return nullToNA(core.getQueueManager().getPlayerQueue(playerId));
            case "position": {
                int p = core.getQueueManager().getQueuePosition(playerId);
                return p >= 0 ? String.valueOf(p) : "N/A";
            }
            case "wait_time": {
                int p = core.getQueueManager().getQueuePosition(playerId);
                return p >= 0 ? (p * 5) + "s" : "0s";
            }
            case "in_queue":
                return String.valueOf(core.getQueueManager().isInQueue(playerId));
            case "elo":
                return String.valueOf(eloOf(playerId));
            case "tier":
                return core.getTierManager().getDisplayName(tierOf(playerId));
            case "wins":
                return numOr(playerId, PlayerData::getWins);
            case "losses":
                return numOr(playerId, PlayerData::getLosses);
            case "winrate": {
                PlayerData d = core.getDatabaseManager().loadPlayer(playerId);
                return d != null ? String.format("%.1f", d.getWinRate()) + "%" : "0%";
            }
            case "total_matches":
                return numOr(playerId, PlayerData::getTotalMatches);
            case "party_size": {
                Party party = core.getPartyManager().getParty(playerId);
                return party != null ? String.valueOf(party.getSize()) : "0";
            }
            case "party_leader": {
                Party party = core.getPartyManager().getParty(playerId);
                if (party == null) {
                    return "N/A";
                }
                MatchPlayer leader = bridge.player(party.getLeader()).orElse(null);
                return leader != null ? leader.getName() : "N/A";
            }
            case "in_party":
                return String.valueOf(core.getPartyManager().isInParty(playerId));
            case "leaderboard_rank": {
                int rank = core.api().getLeaderboardRank(playerId);
                return rank > 0 ? String.valueOf(rank) : "N/A";
            }
            default:
                return "";
        }
    }

    private int eloOf(UUID playerId) {
        PlayerData d = core.getDatabaseManager().loadPlayer(playerId);
        return d != null ? d.getElo() : core.getEloManager().getInitialElo();
    }

    private String tierOf(UUID playerId) {
        PlayerData d = core.getDatabaseManager().loadPlayer(playerId);
        return d != null ? d.getTier() : null;
    }

    private String numOr(UUID playerId, java.util.function.ToIntFunction<PlayerData> getter) {
        PlayerData d = core.getDatabaseManager().loadPlayer(playerId);
        return d != null ? String.valueOf(getter.applyAsInt(d)) : "0";
    }

    private String nullToNA(String s) {
        return s == null ? "N/A" : s;
    }
}
