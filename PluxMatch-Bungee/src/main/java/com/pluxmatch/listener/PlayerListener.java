package com.pluxmatch.listener;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.BungeeMatchPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * 桥接 BungeeCord 玩家生命周期事件到 {@link PluxMatchCore}。
 *
 * @author Reationura
 */
public class PlayerListener implements Listener {

    private final PluxMatchCore core;

    public PlayerListener(PluxMatchCore core) {
        this.core = core;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        core.handlePlayerJoin(new BungeeMatchPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        core.handlePlayerQuit(new BungeeMatchPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        core.handleServerPostConnect(new BungeeMatchPlayer(event.getPlayer()));
    }
}
