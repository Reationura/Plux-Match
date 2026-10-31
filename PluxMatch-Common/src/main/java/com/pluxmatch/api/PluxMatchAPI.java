package com.pluxmatch.api;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.database.LeaderboardEntry;
import com.pluxmatch.event.EventBus;
import com.pluxmatch.match.MatchFilter;
import com.pluxmatch.model.MatchRecord;
import com.pluxmatch.model.Party;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.model.Queue;
import com.pluxmatch.model.QueueResult;
import com.pluxmatch.tier.TierManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * PluxMatch 对外 API（平台无关）。通过 {@code PluxMatchCore#api()} 获取实例。
 *
 * @author Reationura
 */
public class PluxMatchAPI {

    private final PluxMatchCore core;

    public PluxMatchAPI(PluxMatchCore core) {
        this.core = core;
    }

    public EventBus eventBus() {
        return core.eventBus();
    }

    /* ---------- 队列 ---------- */

    public QueueResult joinQueue(UUID playerId, String playerName, String queueId) {
        return core.getQueueManager().joinQueue(playerId, playerName, queueId);
    }

    public boolean leaveQueue(UUID playerId) {
        return core.getQueueManager().leaveQueue(playerId);
    }

    public Queue getQueue(String queueId) {
        return core.getQueueManager().getQueue(queueId);
    }

    public Collection<Queue> getQueues() {
        return core.getQueueManager().getQueues();
    }

    public Collection<Queue> getEnabledQueues() {
        return core.getQueueManager().getEnabledQueues();
    }

    public boolean isInQueue(UUID playerId) {
        return core.getQueueManager().isInQueue(playerId);
    }

    public String getPlayerQueue(UUID playerId) {
        return core.getQueueManager().getPlayerQueue(playerId);
    }

    public int getQueuePosition(UUID playerId) {
        return core.getQueueManager().getQueuePosition(playerId);
    }

    /* ---------- 队伍 ---------- */

    public Party createParty(UUID leader) {
        return core.getPartyManager().createParty(leader);
    }

    public Party getParty(UUID player) {
        return core.getPartyManager().getParty(player);
    }

    public boolean disbandParty(UUID leader) {
        return core.getPartyManager().disbandParty(leader);
    }

    public boolean invitePlayer(UUID inviter, UUID invitee) {
        return core.getPartyManager().invitePlayer(inviter, invitee);
    }

    public boolean acceptInvitation(UUID player, UUID inviter) {
        return core.getPartyManager().acceptInvitation(player, inviter);
    }

    public boolean denyInvitation(UUID player, UUID inviter) {
        return core.getPartyManager().denyInvitation(player, inviter);
    }

    public boolean kickMember(UUID leader, UUID member) {
        return core.getPartyManager().kickMember(leader, member);
    }

    public boolean leaveParty(UUID player) {
        return core.getPartyManager().leaveParty(player);
    }

    public boolean isInParty(UUID player) {
        return core.getPartyManager().isInParty(player);
    }

    public boolean isLeader(UUID player) {
        return core.getPartyManager().isLeader(player);
    }

    public int getPartySize(UUID player) {
        return core.getPartyManager().getPartySize(player);
    }

    /* ---------- 数据 / ELO ---------- */

    public PlayerData getPlayerData(UUID playerId) {
        return core.getDatabaseManager().loadPlayer(playerId);
    }

    public void savePlayerData(PlayerData playerData) {
        core.getDatabaseManager().savePlayer(playerData);
    }

    public List<MatchRecord> getPlayerMatchHistory(UUID playerId, int limit) {
        return core.getDatabaseManager().getPlayerMatchHistory(playerId, limit);
    }

    public int calculateNewElo(int currentElo, int opponentElo, boolean won) {
        return core.getEloManager().calculateNewElo(currentElo, opponentElo, won);
    }

    public void finishMatch(String matchId, List<UUID> winners, List<UUID> losers) {
        core.getMatchManager().finishMatch(matchId, winners, losers);
    }

    public void processMatches() {
        core.getMatchManager().processMatches();
    }

    /* ---------- 段位 / 排行榜（新增） ---------- */

    public TierManager getTierManager() {
        return core.getTierManager();
    }

    public List<LeaderboardEntry> getLeaderboard(boolean byWinRate, int limit) {
        return core.getDatabaseManager().getTopPlayers(byWinRate, limit);
    }

    /**
     * 查询玩家在 ELO 榜中的名次（从 1 开始），未入榜返回 -1。
     */
    public int getLeaderboardRank(UUID playerId) {
        PlayerData data = core.getDatabaseManager().loadPlayer(playerId);
        if (data == null) {
            return -1;
        }
        List<LeaderboardEntry> top = core.getDatabaseManager().getTopPlayers(false, Integer.MAX_VALUE);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).uuid.equals(playerId.toString())) {
                return i + 1;
            }
        }
        return -1;
    }

    /* ---------- 扩展点 ---------- */

    public void registerMatchFilter(MatchFilter filter) {
        core.getMatchManager().registerMatchFilter(filter);
    }

    public void reload() {
        core.reload();
    }
}
