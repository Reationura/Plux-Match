package com.pluxmatch.command;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.BungeeCommandSource;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;

/**
 * BungeeCord 端 {@code /match} 命令薄壳：仅把调用转交给 Common 的 {@link CommandService}。
 *
 * @author Reationura
 */
public class MatchCommand extends Command implements TabExecutor {

    private final PluxMatchCore core;

    public MatchCommand(PluxMatchCore core) {
        super("match", null, "pm", "matchmaking");
        this.core = core;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        core.getCommandService().executeMatch(new BungeeCommandSource(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> list = core.getCommandService().tabCompleteMatch(new BungeeCommandSource(sender), args);
        return list;
    }
}
