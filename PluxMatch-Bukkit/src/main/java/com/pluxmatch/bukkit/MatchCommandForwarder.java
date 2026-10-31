package com.pluxmatch.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 转发 /match 命令到 BungeeCord
 *
 * @author Reationura
 */
public class MatchCommandForwarder implements CommandExecutor {

    private final PluxMatchBukkit plugin;

    public MatchCommandForwarder(PluxMatchBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "这个命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        // 不再检查权限，由 BungeeCord 端统一管理
        // 特定功能的权限（如 VIP 队列）会在 BungeeCord 端的命令执行时检查

        // 构建命令参数
        String commandLine = "match";
        if (args.length > 0) {
            commandLine += " " + String.join(" ", args);
        }

        // 发送到 BungeeCord
        sendCommandToBungeeCord(player, commandLine);

        return true;
    }

    /**
     * 通过 PluginMessage 发送命令到 BungeeCord
     */
    private void sendCommandToBungeeCord(Player player, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("ExecuteCommand");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getName());
        out.writeUTF(command);

        player.sendPluginMessage(plugin, "pluxmatch:command", out.toByteArray());
    }
}