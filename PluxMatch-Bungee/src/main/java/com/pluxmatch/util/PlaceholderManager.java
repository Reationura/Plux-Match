package com.pluxmatch.util;

import com.pluxmatch.PluxMatchCore;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * BungeeCord 端占位符桥接。核心取值逻辑位于 Common 的 {@link PlaceholderService}；
 * 本类仅把 {@code ProxiedPlayer} + 参数转为 {@code (UUID, identifier)} 并转调。
 * <p>
 * PlaceholderAPI 本体面向 Bukkit/Spigot，在纯 BungeeCord 环境并不提供扩展注册点，
 * 故此处保持与历史实现一致的“软桥接”语义：当宿主端有占位消费方时可直接调用
 * {@link #onPlaceholderRequest(ProxiedPlayer, String)} 取值。
 *
 * @author Reationura
 */
public class PlaceholderManager {

    private final PluxMatchCore core;

    public PlaceholderManager(PluxMatchCore core) {
        this.core = core;
    }

    public String onPlaceholderRequest(ProxiedPlayer player, String identifier) {
        if (player == null) {
            return "";
        }
        return core.getPlaceholderService().onRequest(player.getUniqueId(), identifier);
    }
}
