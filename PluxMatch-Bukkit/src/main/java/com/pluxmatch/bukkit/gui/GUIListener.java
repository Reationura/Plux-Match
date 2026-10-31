package com.pluxmatch.bukkit.gui;

import com.pluxmatch.bukkit.PluxMatchBukkit;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * GUI 点击事件监听器
 *
 * @author Reationura
 */
public class GUIListener implements Listener {

    private final PluxMatchBukkit plugin;

    public GUIListener(PluxMatchBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();

        // 处理匹配主菜单
        if (MatchGUI.isMatchGUI(title)) {
            handleMatchGUIClick(event, title);
            return;
        }

        // 处理房间列表
        if (RoomListGUI.isRoomListGUI(title)) {
            handleRoomListClick(event, title);
            return;
        }
    }

    /**
     * 处理匹配主菜单点击（槽位判定，规避旧端彩色物品 getType() 歧义）。
     * 固定槽位：11=随机匹配、 13=房间列表、 15=关闭。
     */
    private void handleMatchGUIClick(InventoryClickEvent event, String title) {
        event.setCancelled(true);

        // 仅处理上方容器（菜单本身）的点击
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        String groupName = MatchGUI.getGroupNameFromTitle(title);
        if (groupName == null) {
            return;
        }

        int slot = event.getRawSlot();

        // 槽位 11 - 随机匹配
        if (slot == 11) {
            sendCommandToBungeeCord(player, "match join " + groupName);
            player.closeInventory();
        }
        // 槽位 13 - 查看房间列表
        else if (slot == 13) {
            requestRoomList(player, groupName);
        }
        // 槽位 15 - 关闭菜单
        else if (slot == 15) {
            player.closeInventory();
        }
    }

    /**
     * 处理房间列表点击（末格 = 返回，槽位判定）。
     */
    private void handleRoomListClick(InventoryClickEvent event, String title) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // 最后一格 - 返回主菜单
        if (slot == topSize - 1) {
            String groupName = RoomListGUI.getGroupNameFromTitle(title);
            if (groupName != null) {
                requestOpenMatchGUI(player, groupName);
            }
        }
    }

    /**
     * 请求打开匹配主菜单
     */
    private void requestOpenMatchGUI(Player player, String groupName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("OpenGUI");
        out.writeUTF(player.getName());
        out.writeUTF(groupName);
        player.sendPluginMessage(plugin, "pluxmatch:command", out.toByteArray());
    }

    /**
     * 请求房间列表
     */
    private void requestRoomList(Player player, String groupName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetRoomList");
        out.writeUTF(player.getName());
        out.writeUTF(groupName);
        player.sendPluginMessage(plugin, "pluxmatch:command", out.toByteArray());
    }

    /**
     * 发送命令到 BungeeCord
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