package com.pluxmatch.platform;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 平台桥接层 SPI。核心逻辑只依赖此接口，具体的 BungeeCord / Velocity 实现提供各方法。
 *
 * @author Reationura
 */
public interface PlatformBridge {

    /* ---------- 玩家 ---------- */

    Optional<MatchPlayer> player(UUID id);

    Optional<MatchPlayer> player(String name);

    Collection<MatchPlayer> onlinePlayers();

    List<String> onlinePlayerNames();

    /* ---------- 消息 / 文本 ---------- */

    void sendMessage(MatchPlayer player, Component message);

    void sendMessage(CommandSource source, Component message);

    void sendTitle(MatchPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut);

    void sendActionBar(MatchPlayer player, Component message);

    /* ---------- 权限 ---------- */

    boolean hasPermission(MatchPlayer player, String node);

    /* ---------- 服务器 / 传送 ---------- */

    void teleport(MatchPlayer player, String serverName);

    /**
     * 异步 ping 指定后端服务器并回调结果。
     */
    void pingServer(String serverName, ServerPingCallback callback);

    boolean hasRegisteredServer(String serverName);

    /* ---------- Plugin 消息通道 ---------- */

    void registerChannel(String channel);

    void sendData(MatchPlayer player, String channel, byte[] payload);

    void broadcastPluginMessage(String channel, byte[] payload);

    /**
     * 以该玩家身份执行代理端命令（供 Bukkit GUI 按钮转发的命令使用，不含前导斜杠）。
     */
    void dispatchCommand(MatchPlayer player, String command);

    /* ---------- 基础设施 ---------- */

    File dataFolder();

    Logger logger();

    /**
     * 服务器 ping 结果回调。
     */
    @FunctionalInterface
    interface ServerPingCallback {
        /**
         * @param online      服务器是否可达
         * @param players     在线人数
         * @param maxPlayers  最大人数
         * @param versionName 版本 / 状态描述，可为 null
         */
        void complete(boolean online, int players, int maxPlayers, String versionName);
    }
}
