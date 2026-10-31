package com.pluxmatch.bukkit;

import com.pluxmatch.bukkit.gui.GUIListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PluxMatch Bukkit 辅助插件
 * 用于将命令转发到 BungeeCord 代理端
 *
 * @author Reationura
 */
public class PluxMatchBukkit extends JavaPlugin {

    private static PluxMatchBukkit instance;
    private BungeeMessageHandler messageHandler;

    @Override
    public void onEnable() {
        instance = this;

        // 注册 PluginMessage 通道
        getServer().getMessenger().registerOutgoingPluginChannel(this, "pluxmatch:command");
        getServer().getMessenger().registerIncomingPluginChannel(this, "pluxmatch:response", new BungeeMessageHandler(this));

        // 注册命令
        getCommand("match").setExecutor(new MatchCommandForwarder(this));
        getCommand("matchadmin").setExecutor(new MatchAdminCommandForwarder(this));

        // 注册 GUI 监听器
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        getLogger().info("PluxMatch-Bukkit has been enabled!");
        getLogger().info("Commands will be forwarded to BungeeCord proxy.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);

        getLogger().info("PluxMatch-Bukkit has been disabled!");
    }

    public static PluxMatchBukkit getInstance() {
        return instance;
    }
}