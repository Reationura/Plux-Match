package com.pluxmatch.platform;

import java.util.UUID;

/**
 * 平台无关的玩家句柄。
 * BungeeCord 端包装 {@code ProxiedPlayer}，Velocity 端包装 {@code Player}。
 *
 * @author Reationura
 */
public interface MatchPlayer {

    UUID getUniqueId();

    String getName();

    /**
     * 玩家当前所在的后端服务器名称，不在任何服务器时返回 null。
     */
    String currentServerName();

    boolean hasPermission(String node);
}
