package com.pluxmatch.command;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.BungeeCommandSource;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

/**
 * BungeeCord 端 {@code /matchadmin} 管理命令薄壳，转交 Common 的 {@link CommandService}。
 *
 * @author Reationura
 */
public class MatchAdminCommand extends Command implements TabExecutor {

    private final PluxMatchCore core;

    public MatchAdminCommand(PluxMatchCore core) {
        super("matchadmin", "pluxmatch.admin", "pma", "matchadm");
        this.core = core;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("pluxmatch.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        core.getCommandService().executeMatchAdmin(new BungeeCommandSource(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> list = core.getCommandService().tabCompleteMatchAdmin(new BungeeCommandSource(sender), args);
        return list;
    }
}
