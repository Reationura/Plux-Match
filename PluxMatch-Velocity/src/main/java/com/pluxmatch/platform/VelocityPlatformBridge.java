package com.pluxmatch.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * {@link PlatformBridge} 的 Velocity 实现。Velocity 原生即 Adventure，组件直接下发；
 * 传送 / ping / 插件消息 / 命令派发均走 Velocity 异步 API。
 *
 * @author Reationura
 */
public class VelocityPlatformBridge implements PlatformBridge {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;
    private final Object plugin;
    private final ConcurrentHashMap<String, ChannelIdentifier> channels = new ConcurrentHashMap<>();

    public VelocityPlatformBridge(ProxyServer proxy, Path dataDirectory, Logger logger, Object plugin) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.plugin = plugin;
    }

    private static VelocityMatchPlayer wrap(Player player) {
        return player == null ? null : new VelocityMatchPlayer(player);
    }

    /* ---------- 玩家 ---------- */

    @Override
    public Optional<MatchPlayer> player(UUID id) {
        return proxy.getPlayer(id).map(VelocityMatchPlayer::new);
    }

    @Override
    public Optional<MatchPlayer> player(String name) {
        return proxy.getPlayer(name).map(VelocityMatchPlayer::new);
    }

    @Override
    public Collection<MatchPlayer> onlinePlayers() {
        List<MatchPlayer> list = new ArrayList<>();
        for (Player p : proxy.getAllPlayers()) {
            list.add(new VelocityMatchPlayer(p));
        }
        return list;
    }

    @Override
    public List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : proxy.getAllPlayers()) {
            names.add(p.getUsername());
        }
        return names;
    }

    /* ---------- 消息 / 文本 ---------- */

    @Override
    public void sendMessage(MatchPlayer player, Component message) {
        Player p = unwrap(player);
        if (p != null) {
            p.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(CommandSource source, Component message) {
        source.sendMessage(message);
    }

    @Override
    public void sendTitle(MatchPlayer player, Component title, Component subtitle,
                          int fadeIn, int stay, int fadeOut) {
        Player p = unwrap(player);
        if (p == null) {
            return;
        }
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L));
        p.showTitle(Title.title(title, subtitle, times));
    }

    @Override
    public void sendActionBar(MatchPlayer player, Component message) {
        Player p = unwrap(player);
        if (p != null) {
            p.sendActionBar(message);
        }
    }

    /* ---------- 权限 ---------- */

    @Override
    public boolean hasPermission(MatchPlayer player, String node) {
        return player != null && player.hasPermission(node);
    }

    /* ---------- 服务器 / 传送 ---------- */

    @Override
    public void teleport(MatchPlayer player, String serverName) {
        Player p = unwrap(player);
        if (p == null) {
            return;
        }
        Optional<RegisteredServer> server = proxy.getServer(serverName);
        server.ifPresent(rs -> p.createConnectionRequest(rs).connect());
    }

    @Override
    public void pingServer(String serverName, ServerPingCallback callback) {
        Optional<RegisteredServer> server = proxy.getServer(serverName);
        if (!server.isPresent()) {
            callback.complete(false, 0, 0, null);
            return;
        }
        server.get().ping().whenComplete((ping, error) -> {
            if (error != null || ping == null) {
                callback.complete(false, 0, 0, null);
                return;
            }
            int online = 0;
            int max = 0;
            Optional<ServerPing.Players> players = ping.getPlayers();
            if (players.isPresent()) {
                online = players.get().getOnline();
                max = players.get().getMax();
            }
            String version = ping.getVersion() == null ? null : ping.getVersion().getName();
            callback.complete(true, online, max, version);
        });
    }

    @Override
    public boolean hasRegisteredServer(String serverName) {
        return proxy.getServer(serverName).isPresent();
    }

    /* ---------- Plugin 消息通道 ---------- */

    @Override
    public void registerChannel(String channel) {
        ChannelIdentifier identifier = channels.computeIfAbsent(channel, MinecraftChannelIdentifier::from);
        proxy.getChannelRegistrar().register(identifier);
    }

    @Override
    public void sendData(MatchPlayer player, String channel, byte[] payload) {
        Player p = unwrap(player);
        if (p == null) {
            return;
        }
        Optional<ServerConnection> current = p.getCurrentServer();
        current.ifPresent(sc -> sc.sendPluginMessage(identifier(channel), payload));
    }

    @Override
    public void broadcastPluginMessage(String channel, byte[] payload) {
        ChannelIdentifier identifier = identifier(channel);
        for (RegisteredServer server : proxy.getAllServers()) {
            server.sendPluginMessage(identifier, payload);
        }
    }

    @Override
    public void dispatchCommand(MatchPlayer player, String command) {
        Player p = unwrap(player);
        if (p != null) {
            proxy.getCommandManager().executeAsync(p, command);
        }
    }

    /* ---------- 基础设施 ---------- */

    @Override
    public File dataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    private ChannelIdentifier identifier(String channel) {
        return channels.computeIfAbsent(channel, MinecraftChannelIdentifier::from);
    }

    private Player unwrap(MatchPlayer player) {
        return player instanceof VelocityMatchPlayer ? ((VelocityMatchPlayer) player).unwrap() : null;
    }
}
