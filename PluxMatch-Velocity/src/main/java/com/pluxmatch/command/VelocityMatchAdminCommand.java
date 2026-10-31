package com.pluxmatch.command;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.VelocityCommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

/**
 * Velocity 端 {@code /matchadmin} 管理命令薄壳，转交 Common 的 {@link CommandService}。
 *
 * @author Reationura
 */
public class VelocityMatchAdminCommand implements SimpleCommand {

    private final PluxMatchCore core;

    public VelocityMatchAdminCommand(PluxMatchCore core) {
        this.core = core;
    }

    @Override
    public void execute(Invocation invocation) {
        core.getCommandService().executeMatchAdmin(
            new VelocityCommandSource(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return core.getCommandService().tabCompleteMatchAdmin(
            new VelocityCommandSource(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("pluxmatch.admin");
    }
}
