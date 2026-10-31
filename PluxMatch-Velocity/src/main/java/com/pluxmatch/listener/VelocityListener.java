package com.pluxmatch.listener;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.messaging.MessageHandler;
import com.pluxmatch.platform.VelocityMatchPlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;

/**
 * 桥接 Velocity 事件到 {@link PluxMatchCore}：玩家进出、连服后确认、入站插件消息路由。
 *
 * @author Reationura
 */
public class VelocityListener {

    private final PluxMatchCore core;

    public VelocityListener(PluxMatchCore core) {
        this.core = core;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        core.handlePlayerJoin(new VelocityMatchPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        core.handlePlayerQuit(new VelocityMatchPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        core.handleServerPostConnect(new VelocityMatchPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        String id = event.getIdentifier().getId();
        if (MessageHandler.DATA_CHANNEL.equals(id) || MessageHandler.COMMAND_CHANNEL.equals(id)) {
            core.getMessageHandler().handle(id, null, event.getData());
        }
    }
}
