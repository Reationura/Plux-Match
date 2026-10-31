package com.pluxmatch.platform;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * BungeeCord 端的 {@link com.pluxmatch.platform.CommandSource} 实现，包装代理 {@link CommandSender}
 * （兼容控制台与玩家）。Adventure 组件先序列化为带 § 的旧式字符串再交给 Bungee 发送。
 *
 * @author Reationura
 */
public class BungeeCommandSource implements CommandSource {

    private final CommandSender sender;

    public BungeeCommandSource(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public void sendMessage(Component message) {
        String legacy = BungeeText.toLegacy(message);
        sender.sendMessage(new TextComponent(legacy));
    }

    @Override
    public boolean hasPermission(String node) {
        return sender.hasPermission(node);
    }

    @Override
    public MatchPlayer asPlayer() {
        if (sender instanceof ProxiedPlayer) {
            return new BungeeMatchPlayer((ProxiedPlayer) sender);
        }
        return null;
    }
}
