package com.pluxmatch.listener;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.messaging.MessageHandler;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * 桥接 BungeeCord 入站 Plugin Message 到 {@link MessageHandler}。
 * 只处理后端上报的结果通道与命令转发通道，其余忽略。
 *
 * @author Reationura
 */
public class PluginMessageListener implements Listener {

    private final PluxMatchCore core;

    public PluginMessageListener(PluxMatchCore core) {
        this.core = core;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        String tag = event.getTag();
        if (MessageHandler.DATA_CHANNEL.equals(tag) || MessageHandler.COMMAND_CHANNEL.equals(tag)) {
            event.setCancelled(true);
            core.getMessageHandler().handle(tag, null, event.getData());
        }
    }
}
