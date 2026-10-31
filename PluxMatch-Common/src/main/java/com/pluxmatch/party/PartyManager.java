package com.pluxmatch.party;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.model.Party;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.util.MapUtils;
import com.pluxmatch.util.MessageManager;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 队伍管理（平台无关）。
 *
 * @author Reationura
 */
public class PartyManager {

    private final PluxMatchCore core;
    private final PlatformBridge bridge;
    private final MessageManager messages;

    private final ConcurrentHashMap<UUID, Party> parties;

    public PartyManager(PluxMatchCore core) {
        this.core = core;
        this.bridge = core.bridge();
        this.messages = core.getMessageManager();
        this.parties = new ConcurrentHashMap<>();
    }

    public Party createParty(UUID leader) {
        if (getParty(leader) != null) {
            return null;
        }

        Party party = new Party(leader);
        parties.put(leader, party);

        return party;
    }

    public Party getParty(UUID player) {
        for (Party party : parties.values()) {
            if (party.isMember(player)) {
                return party;
            }
        }
        return null;
    }

    public boolean disbandParty(UUID leader) {
        Party party = parties.remove(leader);
        if (party == null) {
            return false;
        }

        for (UUID member : party.getMembers()) {
            if (!member.equals(leader)) {
                bridge.player(member).ifPresent(p -> messages.sendMessage(p, "party.disband-success"));
            }
        }

        return true;
    }

    public boolean invitePlayer(UUID inviter, UUID invitee) {
        Party party = getParty(inviter);
        if (party == null || !party.isLeader(inviter) || party.isMember(invitee)) {
            return false;
        }

        party.sendInvitation(invitee);

        MatchPlayer target = bridge.player(invitee).orElse(null);
        if (target != null) {
            MatchPlayer sender = bridge.player(inviter).orElse(null);
            messages.sendMessage(target, "party.invite-received",
                MapUtils.of("player", sender != null ? sender.getName() : "Unknown"));
        }

        return true;
    }

    public boolean acceptInvitation(UUID player, UUID inviter) {
        Party party = getParty(inviter);
        if (party == null || !party.hasInvitation(player)) {
            return false;
        }

        party.addMember(player);
        party.removeInvitation(player);

        MatchPlayer newMember = bridge.player(player).orElse(null);
        String name = newMember != null ? newMember.getName() : "Unknown";
        for (UUID member : party.getMembers()) {
            bridge.player(member).ifPresent(p -> messages.sendMessage(p, "party.member-joined", MapUtils.of("player", name)));
        }

        return true;
    }

    public boolean denyInvitation(UUID player, UUID inviter) {
        Party party = getParty(inviter);
        if (party == null) {
            return false;
        }

        party.removeInvitation(player);

        MatchPlayer target = bridge.player(inviter).orElse(null);
        if (target != null) {
            MatchPlayer sender = bridge.player(player).orElse(null);
            messages.sendMessage(target, "party.invite-denied",
                MapUtils.of("player", sender != null ? sender.getName() : "Unknown"));
        }

        return true;
    }

    public boolean kickMember(UUID leader, UUID member) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader) || !party.isMember(member) || party.isLeader(member)) {
            return false;
        }

        party.removeMember(member);

        if (party.isDisbanded()) {
            parties.remove(leader);
        }

        MatchPlayer kicked = bridge.player(member).orElse(null);
        String name = kicked != null ? kicked.getName() : "Unknown";
        if (kicked != null) {
            messages.sendMessage(kicked, "party.member-left", MapUtils.of("player", name));
        }
        for (UUID m : party.getMembers()) {
            bridge.player(m).ifPresent(p -> messages.sendMessage(p, "party.member-left", MapUtils.of("player", name)));
        }

        return true;
    }

    public boolean leaveParty(UUID player) {
        Party party = getParty(player);
        if (party == null) {
            return false;
        }

        if (party.isLeader(player)) {
            return disbandParty(player);
        }

        party.removeMember(player);

        MatchPlayer leaving = bridge.player(player).orElse(null);
        String name = leaving != null ? leaving.getName() : "Unknown";
        for (UUID member : party.getMembers()) {
            bridge.player(member).ifPresent(p -> messages.sendMessage(p, "party.member-left", MapUtils.of("player", name)));
        }

        return true;
    }

    public Set<UUID> getMembers(UUID player) {
        Party party = getParty(player);
        return party != null ? party.getMembers() : Collections.emptySet();
    }

    public UUID getLeader(UUID player) {
        Party party = getParty(player);
        return party != null ? party.getLeader() : null;
    }

    public int getPartySize(UUID player) {
        Party party = getParty(player);
        return party != null ? party.getSize() : 0;
    }

    public boolean isInParty(UUID player) {
        return getParty(player) != null;
    }

    public boolean isLeader(UUID player) {
        Party party = getParty(player);
        return party != null && party.isLeader(player);
    }

    public void cleanupOfflinePlayers() {
        for (Party party : parties.values()) {
            for (UUID member : party.getMembers()) {
                if (!bridge.player(member).isPresent()) {
                    party.removeMember(member);
                }
            }

            if (party.isDisbanded()) {
                parties.remove(party.getLeader());
            }
        }
    }
}
