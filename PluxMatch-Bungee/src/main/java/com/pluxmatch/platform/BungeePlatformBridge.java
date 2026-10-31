package com.pluxmatch.platform;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * {@link PlatformBridge} 的 BungeeCord 实现。Adventure 组件统一序列化为 § 旧式字符串后，
 * 通过 Bungee 自带的 {@code TextComponent}/{@code sendMessage}/{@code createTitle} 下发，
 * 从而无需在运行期打包完整 Adventure 实现。
 *
 * @author Reationura
 */
public class BungeePlatformBridge implements PlatformBridge {

    private final ProxyServer proxy;
    private final File dataFolder;
    private final Logger logger;

    public BungeePlatformBridge(net.md_5.bungee.api.plugin.Plugin plugin) {
        this.proxy = plugin.getProxy();
        this.dataFolder = plugin.getDataFolder();
        this.logger = plugin.getLogger();
    }

    private static BungeeMatchPlayer wrap(ProxiedPlayer player) {
        return player == null ? null : new BungeeMatchPlayer(player);
    }

    /* ---------- 玩家 ---------- */

    @Override
    public Optional<MatchPlayer> player(UUID id) {
        return Optional.ofNullable(wrap(proxy.getPlayer(id)));
    }

    @Override
    public Optional<MatchPlayer> player(String name) {
        return Optional.ofNullable(wrap(proxy.getPlayer(name)));
    }

    @Override
    public Collection<MatchPlayer> onlinePlayers() {
        List<MatchPlayer> list = new ArrayList<>();
        for (ProxiedPlayer p : proxy.getPlayers()) {
            list.add(new BungeeMatchPlayer(p));
        }
        return list;
    }

    @Override
    public List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (ProxiedPlayer p : proxy.getPlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    /* ---------- 消息 / 文本 ---------- */

    @Override
    public void sendMessage(MatchPlayer player, Component message) {
        ProxiedPlayer pp = unwrap(player);
        if (pp != null) {
            pp.sendMessage(new TextComponent(BungeeText.toLegacy(message)));
        }
    }

    @Override
    public void sendMessage(CommandSource source, Component message) {
        source.sendMessage(message);
    }

    @Override
    public void sendTitle(MatchPlayer player, Component title, Component subtitle,
                          int fadeIn, int stay, int fadeOut) {
        ProxiedPlayer pp = unwrap(player);
        if (pp == null) {
            return;
        }
        try {
            Title t = proxy.createTitle()
                .title(new TextComponent(BungeeText.toLegacy(title)))
                .subTitle(new TextComponent(BungeeText.toLegacy(subtitle)))
                .fadeIn(fadeIn)
                .stay(stay)
                .fadeOut(fadeOut);
            t.send(pp);
        } catch (Throwable t) {
            // 老构建无 proxy.createTitle() → 退化为普通聊天消息
            String legacyTitle = BungeeText.toLegacy(title);
            String legacySub = BungeeText.toLegacy(subtitle);
            if (!legacyTitle.isEmpty()) {
                pp.sendMessage(new TextComponent(legacyTitle));
            }
            if (!legacySub.isEmpty()) {
                pp.sendMessage(new TextComponent(legacySub));
            }
        }
    }

    @Override
    public void sendActionBar(MatchPlayer player, Component message) {
        ProxiedPlayer pp = unwrap(player);
        if (pp == null) {
            return;
        }
        try {
            pp.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(BungeeText.toLegacy(message)));
        } catch (Throwable t) {
            // 老构建无 ChatMessageType.ACTION_BAR → 静默跳过
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
        ProxiedPlayer pp = unwrap(player);
        if (pp == null) {
            return;
        }
        ServerInfo info = proxy.getServerInfo(serverName);
        if (info != null) {
            pp.connect(info);
        }
    }

    @Override
    public void pingServer(String serverName, ServerPingCallback callback) {
        ServerInfo info = proxy.getServerInfo(serverName);
        if (info == null) {
            callback.complete(false, 0, 0, null);
            return;
        }
        info.ping((ping, error) -> {
            if (error != null || ping == null) {
                callback.complete(false, 0, 0, null);
                return;
            }
            int online = 0;
            int max = 0;
            if (ping.getPlayers() != null) {
                online = ping.getPlayers().getOnline();
                max = ping.getPlayers().getMax();
            }
            String version = ping.getVersion() == null ? null : ping.getVersion().getName();
            callback.complete(true, online, max, version);
        });
    }

    @Override
    public boolean hasRegisteredServer(String serverName) {
        return proxy.getServerInfo(serverName) != null;
    }

    /* ---------- Plugin 消息通道 ---------- */

    @Override
    public void registerChannel(String channel) {
        proxy.registerChannel(channel);
    }

    @Override
    public void sendData(MatchPlayer player, String channel, byte[] payload) {
        ProxiedPlayer pp = unwrap(player);
        if (pp == null) {
            return;
        }
        Server server = pp.getServer();
        if (server != null) {
            server.sendData(channel, payload);
        }
    }

    @Override
    public void broadcastPluginMessage(String channel, byte[] payload) {
        for (ServerInfo info : proxy.getServers().values()) {
            info.sendData(channel, payload);
        }
    }

    @Override
    public void dispatchCommand(MatchPlayer player, String command) {
        ProxiedPlayer pp = unwrap(player);
        if (pp != null) {
            proxy.getPluginManager().dispatchCommand(pp, command);
        }
    }

    /* ---------- 基础设施 ---------- */

    @Override
    public File dataFolder() {
        return dataFolder;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    private ProxiedPlayer unwrap(MatchPlayer player) {
        return player instanceof BungeeMatchPlayer ? ((BungeeMatchPlayer) player).unwrap() : null;
    }
}
