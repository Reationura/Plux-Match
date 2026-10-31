package com.pluxmatch.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.Optional;
import java.util.UUID;

/**
 * Velocity 端的 {@link MatchPlayer} 实现，包装 {@link Player}。
 *
 * @author Reationura
 */
public class VelocityMatchPlayer implements MatchPlayer {

    private final Player player;

    public VelocityMatchPlayer(Player player) {
        this.player = player;
    }

    public Player unwrap() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public String currentServerName() {
        Optional<ServerConnection> current = player.getCurrentServer();
        return current.map(sc -> sc.getServerInfo().getName()).orElse(null);
    }

    @Override
    public boolean hasPermission(String node) {
        return player.hasPermission(node);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VelocityMatchPlayer
            && ((VelocityMatchPlayer) obj).player.getUniqueId().equals(player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }
}
