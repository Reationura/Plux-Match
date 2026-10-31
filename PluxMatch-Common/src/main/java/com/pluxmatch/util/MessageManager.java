package com.pluxmatch.util;

import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.platform.CommandSource;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;

/**
 * 消息管理（平台无关），基于 Adventure Component。
 *
 * @author Reationura
 */
public class MessageManager {

    private final PlatformBridge bridge;
    private final ConfigManager configManager;
    private final String prefix;
    private final boolean useColorCodes;

    public MessageManager(PlatformBridge bridge, ConfigManager configManager) {
        this.bridge = bridge;
        this.configManager = configManager;

        Map<String, Object> config = configManager.getConfig();
        this.prefix = configManager.getString(config, "messages.prefix", "&6[PluxMatch] ");
        this.useColorCodes = configManager.getBoolean(config, "messages.use-color-codes", true);
    }

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public Component getMessage(String path) {
        Map<String, Object> messages = configManager.getMessages();
        String message = configManager.getString(messages, path, "");
        return legacy(prefix + message);
    }

    public Component getMessage(String path, Map<String, String> replacements) {
        Map<String, Object> messages = configManager.getMessages();
        String message = configManager.getString(messages, path, "");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return legacy(prefix + message);
    }

    /**
     * 不带前缀的原始消息（用于帮助/列表等自定义排版）。
     */
    public Component raw(String text) {
        return legacy(text);
    }

    public void sendMessage(CommandSource source, String path) {
        bridge.sendMessage(source, getMessage(path));
    }

    public void sendMessage(CommandSource source, String path, Map<String, String> replacements) {
        bridge.sendMessage(source, getMessage(path, replacements));
    }

    public void sendRaw(CommandSource source, String text) {
        bridge.sendMessage(source, legacy(text));
    }

    public void sendMessage(MatchPlayer player, String path) {
        bridge.sendMessage(player, getMessage(path));
    }

    public void sendMessage(MatchPlayer player, String path, Map<String, String> replacements) {
        bridge.sendMessage(player, getMessage(path, replacements));
    }

    public void sendTitle(MatchPlayer player, String title, String subtitle) {
        bridge.sendTitle(player, legacy(title), legacy(subtitle), 10, 40, 10);
    }

    public void sendActionBar(MatchPlayer player, String message) {
        bridge.sendActionBar(player, legacy(message));
    }

    public String getPrefix() {
        return prefix;
    }
}
