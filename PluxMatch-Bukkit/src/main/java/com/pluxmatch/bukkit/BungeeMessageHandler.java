package com.pluxmatch.bukkit;

import com.pluxmatch.bukkit.compat.TitleSender;
import com.pluxmatch.bukkit.gui.MatchGUI;
import com.pluxmatch.bukkit.gui.RoomListGUI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理来自 BungeeCord 的响应消息
 *
 * @author Reationura
 */
public class BungeeMessageHandler implements PluginMessageListener {

    private final PluxMatchBukkit plugin;

    public BungeeMessageHandler(PluxMatchBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        plugin.getLogger().info("Received message on channel: " + channel);

        if (!channel.equals("pluxmatch:response")) {
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            plugin.getLogger().info("SubChannel: " + subChannel);

            switch (subChannel) {
                case "Message":
                    handleMessage(in);
                    break;
                case "OpenGUI":
                    handleOpenGUI(in);
                    break;
                case "RoomList":
                    handleRoomList(in);
                    break;
                case "MatchSuccess":
                    handleMatchSuccess(in);
                    break;
                case "MatchFailed":
                    handleMatchFailed(in);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing plugin message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理普通消息
     */
    private void handleMessage(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String text = in.readUTF();

        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null) {
            target.sendMessage(text);
        }
    }

    /**
     * 处理打开 GUI 请求
     */
    private void handleOpenGUI(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String groupName = in.readUTF();

        plugin.getLogger().info("OpenGUI request for " + playerName + " with group: " + groupName);

        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null) {
            MatchGUI.openMatchGUI(target, groupName);
            plugin.getLogger().info("Opened match GUI for " + playerName + " with group: " + groupName);
        } else {
            plugin.getLogger().warning("Player not found: " + playerName);
        }
    }

    /**
     * 处理房间列表
     */
    private void handleRoomList(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String groupName = in.readUTF();

        List<String> rooms = new ArrayList<>();
        try {
            int roomCount = in.readInt();
            for (int i = 0; i < roomCount; i++) {
                rooms.add(in.readUTF());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading room list: " + e.getMessage());
        }

        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null) {
            RoomListGUI.openRoomListGUI(target, groupName, rooms);
            plugin.getLogger().info("Opened room list GUI for " + playerName + " with " + rooms.size() + " rooms");
        }
    }

    /**
     * 处理匹配成功
     */
    private void handleMatchSuccess(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String title = in.readUTF();
        String subtitle = in.readUTF();
        String message = in.readUTF();

        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null) {
            // 转换颜色代码
            title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            subtitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle);

            // 发送 Title（版本降级）
            TitleSender.send(target, title, subtitle);
            // 发送消息
            if (!message.isEmpty()) {
                message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
                target.sendMessage(message);
            }
        }
    }

    /**
     * 处理匹配失败
     */
    private void handleMatchFailed(ByteArrayDataInput in) {
        String playerName = in.readUTF();
        String title = in.readUTF();
        String subtitle = in.readUTF();
        String message = in.readUTF();

        Player target = plugin.getServer().getPlayer(playerName);
        if (target != null) {
            // 转换颜色代码
            title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
            subtitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle);

            // 发送 Title（版本降级）
            TitleSender.send(target, title, subtitle);
            // 发送消息
            if (!message.isEmpty()) {
                message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
                target.sendMessage(message);
            }
        }
    }
}