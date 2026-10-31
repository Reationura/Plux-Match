package com.pluxmatch;

import com.pluxmatch.command.VelocityMatchAdminCommand;
import com.pluxmatch.command.VelocityMatchCommand;
import com.pluxmatch.listener.VelocityListener;
import com.pluxmatch.platform.VelocityPlatformBridge;
import com.pluxmatch.util.UpdateChecker;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Velocity 入口薄壳：在 {@link ProxyInitializeEvent} 构造 {@link VelocityPlatformBridge}
 * 并交给平台无关 {@link PluxMatchCore}，事件与命令全部转调 core。
 *
 * @author Reationura
 */
@Plugin(
    id = "pluxmatch",
    name = "PluxMatch",
    version = "1.5.2",
    description = "Advanced Matchmaking Plugin",
    authors = {"Reationura"}
)
public class PluxMatchVelocity {

    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;

    private PluxMatchCore core;

    @Inject
    public PluxMatchVelocity(ProxyServer proxy, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = Logger.getLogger("PluxMatch");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("========================================");
        logger.info("PluxMatch v1.5.2 (Velocity)");
        logger.info("Author: Reationura");
        logger.info("========================================");

        core = new PluxMatchCore(
            new VelocityPlatformBridge(proxy, dataDirectory, logger, this), "1.5.2");

        proxy.getEventManager().register(this, new VelocityListener(core));
        registerCommands();

        new UpdateChecker(core, core.version()).check();
        logger.info("PluxMatch has been enabled!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.shutdown();
        }
        logger.info("PluxMatch has been disabled!");
    }

    private void registerCommands() {
        CommandManager cm = proxy.getCommandManager();
        cm.register(cm.metaBuilder("match").aliases("pm", "matchmaking").plugin(this).build(),
            new VelocityMatchCommand(core));
        cm.register(cm.metaBuilder("matchadmin").aliases("pma", "matchadm").plugin(this).build(),
            new VelocityMatchAdminCommand(core));
    }

    public PluxMatchCore getCore() {
        return core;
    }
}
