package com.pluxmatch.bukkit.compat;

import org.bukkit.Bukkit;

/**
 * 服务端版本判定（跨 1.7 → 最新）。解析 {@link Bukkit#getBukkitVersion()}
 * （形如 {@code 1.20.6-R0.1-SNAPSHOT} / {@code 1.8.8} / {@code 1.7.10}）取 major/minor。
 * 解析失败按现代版本处理，优先保证新版行为。
 *
 * @author Reationura
 */
public final class ServerVersion {

    private static final int MAJOR;
    private static final int MINOR;

    static {
        int major = 1;
        int minor = 20;
        try {
            String raw = Bukkit.getBukkitVersion(); // "1.20.6-R0.1-SNAPSHOT"
            String core = raw.split("-")[0];          // "1.20.6"
            String[] parts = core.split("\\.");
            if (parts.length >= 2) {
                major = Integer.parseInt(parts[0]);
                minor = Integer.parseInt(parts[1]);
            } else if (parts.length == 1) {
                major = Integer.parseInt(parts[0]);
            }
        } catch (Throwable t) {
            // 解析失败保持现代默认值
        }
        MAJOR = major;
        MINOR = minor;
    }

    private ServerVersion() {
    }

    /**
     * 当前服务端是否 &gt;= 指定的 major.minor。
     */
    public static boolean isAtLeast(int major, int minor) {
        if (MAJOR != major) {
            return MAJOR > major;
        }
        return MINOR >= minor;
    }
}
