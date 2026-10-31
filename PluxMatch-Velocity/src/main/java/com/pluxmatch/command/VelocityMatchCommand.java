package com.pluxmatch.command;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.VelocityCommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

/**
 * Velocity 端 {@code /match} 命令薄壳：转交 Common 的 {@link CommandService}。
 *
 * @author Reationura
 */
public class VelocityMatchCommand implements SimpleCommand {

    private final PluxMatchCore core;

    public VelocityMatchCommand(PluxMatchCore core) {
        this.core = core;
    }

    @Override
    public void execute(Invocation invocation) {
        core.getCommandService().executeMatch(
            new VelocityCommandSource(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return core.getCommandService().tabCompleteMatch(
            new VelocityCommandSource(invocation.source()), invocation.arguments());
    }
}
