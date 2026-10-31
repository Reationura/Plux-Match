package com.pluxmatch.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 转发 /matchadmin 命令到 BungeeCord
 *
 * @author Reationura
 */
public class MatchAdminCommandForwarder implements CommandExecutor {

    private final PluxMatchBukkit plugin;

    public MatchAdminCommandForwarder(PluxMatchBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // 控制台执行的命令直接在 BungeeCord 端处理，不需要转发
            sender.sendMessage(ChatColor.RED + "控制台请直接在 BungeeCord 控制台执行此命令！");
            return true;
        }

        Player player = (Player) sender;

        // 检查权限
        if (!player.hasPermission("match.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c你没有权限执行此命令！"));
            return true;
        }

        // 构建命令参数
        String commandLine = "matchadmin";
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