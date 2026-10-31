package com.pluxmatch.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 组队模型（平台无关）。
 *
 * @author Reationura
 */
public class Party {

    private final UUID leader;
    private final Set<UUID> members;
    private final ConcurrentHashMap<UUID, Long> invitations;
    private final ConcurrentHashMap<UUID, Long> pendingRequests;
    private long creationTime;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = ConcurrentHashMap.newKeySet();
        this.members.add(leader);
        this.invitations = new ConcurrentHashMap<>();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.creationTime = System.currentTimeMillis();
    }

    public UUID getLeader() {
        return leader;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public int getSize() {
        return members.size();
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public boolean isMember(UUID player) {
        return members.contains(player);
    }

    public boolean addMember(UUID player) {
        if (isMember(player)) {
            return false;
        }
        return members.add(player);
    }

    public boolean removeMember(UUID player) {
        return members.remove(player);
    }

    public boolean isDisbanded() {
        return members.isEmpty() || !members.contains(leader);
    }

    public void sendInvitation(UUID player) {
        invitations.put(player, System.currentTimeMillis());
    }

    public boolean hasInvitation(UUID player) {
        Long time = invitations.get(player);
        if (time == null) {
            return false;
        }
        return System.currentTimeMillis() - time < 60000;
    }

    public void removeInvitation(UUID player) {
        invitations.remove(player);
    }

    public void clearInvitations() {
        invitations.clear();
    }

    public void addPendingRequest(UUID player) {
        pendingRequests.put(player, System.currentTimeMillis());
    }

    public boolean hasPendingRequest(UUID player) {
        Long time = pendingRequests.get(player);
        if (time == null) {
            return false;
        }
        return System.currentTimeMillis() - time < 30000;
    }

    public void removePendingRequest(UUID player) {
        pendingRequests.remove(player);
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }
}
