package com.pluxmatch.platform;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

/**
 * Velocity 端的 {@link com.pluxmatch.platform.CommandSource} 实现。Velocity 原生使用 Adventure，
 * 故 {@link Component} 直接下发，无需序列化。
 *
 * @author Reationura
 */
public class VelocityCommandSource implements com.pluxmatch.platform.CommandSource {

    private final CommandSource source;

    public VelocityCommandSource(CommandSource source) {
        this.source = source;
    }

    @Override
    public String getName() {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return "CONSOLE";
    }

    @Override
    public void sendMessage(Component message) {
        source.sendMessage(message);
    }

    @Override
    public boolean hasPermission(String node) {
        return source.hasPermission(node);
    }

    @Override
    public MatchPlayer asPlayer() {
        if (source instanceof Player) {
            return new VelocityMatchPlayer((Player) source);
        }
        return null;
    }
}
