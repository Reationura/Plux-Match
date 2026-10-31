package com.pluxmatch.platform;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Adventure 组件与 BungeeCord 旧式（§）文本之间的转换工具。
 * <p>
 * BungeeCord 运行期自带的聊天 API 只认 § 颜色码，故统一先把 Adventure 组件序列化成
 * legacySection 字符串，再交给 {@code TextComponent}/{@code sendMessage(String)}，
 * 避免打包进完整 adventure 序列化器带来的版本冲突。
 *
 * @author Reationura
 */
final class BungeeText {

    private BungeeText() {
    }

    /**
     * 将 Adventure 组件转为带 § 颜色码的字符串。
     */
    static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
