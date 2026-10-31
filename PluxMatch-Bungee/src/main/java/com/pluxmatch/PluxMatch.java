package com.pluxmatch;

import com.pluxmatch.api.PluxMatchAPI;
import com.pluxmatch.command.MatchAdminCommand;
import com.pluxmatch.command.MatchCommand;
import com.pluxmatch.listener.PlayerListener;
import com.pluxmatch.listener.PluginMessageListener;
import com.pluxmatch.platform.BungeePlatformBridge;
import com.pluxmatch.util.UpdateChecker;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

/**
 * BungeeCord 入口薄壳：仅构造 {@link BungeePlatformBridge} 并交给平台无关的
 * {@link PluxMatchCore}，命令与监听器全部转调 core。
 *
 * @author Reationura
 */
public class PluxMatch extends Plugin {

    private static PluxMatch instance;
    private PluxMatchCore core;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("========================================");
        getLogger().info("PluxMatch v" + getDescription().getVersion());
        getLogger().info("Author: Reationura");
        getLogger().info("========================================");

        core = new PluxMatchCore(new BungeePlatformBridge(this), getDescription().getVersion());

        registerCommands();
        registerListeners();

        new UpdateChecker(core, core.version()).check();
        getLogger().info("PluxMatch has been enabled!");
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
        getLogger().info("PluxMatch has been disabled!");
    }

    private void registerCommands() {
        PluginManager pm = getProxy().getPluginManager();
        pm.registerCommand(this, new MatchCommand(core));
        pm.registerCommand(this, new MatchAdminCommand(core));
    }

    private void registerListeners() {
        PluginManager pm = getProxy().getPluginManager();
        pm.registerListener(this, new PlayerListener(core));
        pm.registerListener(this, new PluginMessageListener(core));
    }

    public static PluxMatch getInstance() {
        return instance;
    }

    public PluxMatchCore getCore() {
        return core;
    }

    public PluxMatchAPI getAPI() {
        return core.api();
    }
}
