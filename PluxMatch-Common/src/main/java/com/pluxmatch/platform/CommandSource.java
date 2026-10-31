package com.pluxmatch.platform;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * 命令发送者抽象，兼容玩家与控制台。
 *
 * @author Reationura
 */
public interface CommandSource {

    String getName();

    void sendMessage(Component message);

    boolean hasPermission(String node);

    /**
     * 若发送者为在线玩家则返回其句柄，否则返回 null（如控制台）。
     */
    MatchPlayer asPlayer();

    default boolean isPlayer() {
        return asPlayer() != null;
    }

    /**
     * 便捷方法：按玩家 UUID 获取在线玩家句柄（供命令参数补全/查找）。
     */
    default MatchPlayer lookupPlayer(UUID id) {
        return null;
    }
}
