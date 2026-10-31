package com.pluxmatch.platform;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.UUID;

/**
 * BungeeCord 端的 {@link MatchPlayer} 实现，包装 {@link ProxiedPlayer}。
 *
 * @author Reationura
 */
public class BungeeMatchPlayer implements MatchPlayer {

    private final ProxiedPlayer player;

    public BungeeMatchPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    public ProxiedPlayer unwrap() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public String currentServerName() {
        Server server = player.getServer();
        if (server == null || server.getInfo() == null) {
            return null;
        }
        return server.getInfo().getName();
    }

    @Override
    public boolean hasPermission(String node) {
        return player.hasPermission(node);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BungeeMatchPlayer
            && ((BungeeMatchPlayer) obj).player.getUniqueId().equals(player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return player.getUniqueId().hashCode();
    }
}
