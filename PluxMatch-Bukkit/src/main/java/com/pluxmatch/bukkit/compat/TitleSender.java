package com.pluxmatch.bukkit.compat;

import org.bukkit.entity.Player;

/**
 * Title 发送降级层（跨 1.7 → 最新）。
 * <p>
 * {@code sendTitle(String,String,int,int,int)} 仅 {@code >=1.14}、
 * {@code sendTitle(String,String)} 仅 {@code >=1.8}、{@code 1.7} 无 Title API。
 * 各调用置于版本分支内，旧端不执行即不触发 {@code NoSuchMethodError}
 * （JVM 惰性解析方法引用，仅在实际 invoke 时才校验）。
 *
 * @author Reationura
 */
public final class TitleSender {

    private TitleSender() {
    }

    /**
     * 向玩家发送 Title，按服务端能力自动降级。
     *
     * @param player   目标玩家
     * @param title    主标题（已含颜色代码）
     * @param subtitle 副标题（已含颜色代码）
     */
    public static void send(Player player, String title, String subtitle) {
        try {
            if (ServerVersion.isAtLeast(1, 14)) {
                player.sendTitle(title, subtitle, 10, 70, 20);
            } else if (ServerVersion.isAtLeast(1, 8)) {
                player.sendTitle(title, subtitle);
            } else {
                // 1.7：无 Title API，降级为聊天消息
                if (title != null && !title.isEmpty()) {
                    player.sendMessage(title);
                }
                if (subtitle != null && !subtitle.isEmpty()) {
                    player.sendMessage(subtitle);
                }
            }
        } catch (Throwable t) {
            // 极端兜底：任何反射/方法缺失都退化为普通消息
            try {
                if (title != null && !title.isEmpty()) {
                    player.sendMessage(title);
                }
            } catch (Throwable ignored) {
                // 忽略
            }
        }
    }
}
