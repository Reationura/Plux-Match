package com.pluxmatch.bukkit.gui;

import com.pluxmatch.bukkit.compat.GuiIcon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 房间列表 GUI
 *
 * @author Reationura
 */
public class RoomListGUI {

    private static final String GUI_TITLE_PREFIX = "房间列表: ";

    /**
     * 打开房间列表 GUI
     *
     * @param player 玩家
     * @param groupName 组名称
     * @param rooms 房间信息列表（格式：服务器名|状态|玩家数|最大玩家数）
     */
    public static void openRoomListGUI(Player player, String groupName, List<String> rooms) {
        String title = GUI_TITLE_PREFIX + groupName;

        // 计算需要的行数（每行9格）
        int rows = Math.max(1, (int) Math.ceil((rooms.size() + 1) / 9.0));
        rows = Math.min(rows, 6); // 最大6行

        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);

        // 添加房间物品
        for (int i = 0; i < rooms.size() && i < rows * 9 - 1; i++) {
            String roomData = rooms.get(i);
            String[] parts = roomData.split("\\|");

            String serverName = parts.length > 0 ? parts[0] : "Unknown";
            String status = parts.length > 1 ? parts[1] : "UNKNOWN";
            int playerCount = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            int maxPlayers = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;

            ItemStack roomItem = createRoomItem(serverName, status, playerCount, maxPlayers);
            inventory.setItem(i, roomItem);
        }

        // 返回按钮（最后一个槽位）
        ItemStack back = GuiIcon.create("ARROW");
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "返回");
        back.setItemMeta(backMeta);
        inventory.setItem(rows * 9 - 1, back);

        player.openInventory(inventory);
    }

    /**
     * 创建房间物品
     */
    private static ItemStack createRoomItem(String serverName, String status, int playerCount, int maxPlayers) {
        String materialName;
        String statusText;
        String statusColor;

        switch (status.toUpperCase()) {
            case "ONLINE":
            case "RUNNING":
                materialName = "LIME_WOOL";
                statusText = "运行中";
                statusColor = ChatColor.GREEN.toString();
                break;
            case "WAITING":
                materialName = "YELLOW_WOOL";
                statusText = "等待中";
                statusColor = ChatColor.YELLOW.toString();
                break;
            case "STARTING":
                materialName = "ORANGE_WOOL";
                statusText = "即将开始";
                statusColor = ChatColor.GOLD.toString();
                break;
            case "INGAME":
                materialName = "RED_WOOL";
                statusText = "游戏中";
                statusColor = ChatColor.RED.toString();
                break;
            case "OFFLINE":
            default:
                materialName = "GRAY_WOOL";
                statusText = "离线";
                statusColor = ChatColor.GRAY.toString();
                break;
        }

        ItemStack item = GuiIcon.create(materialName);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + serverName);
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "状态: " + statusColor + statusText,
            ChatColor.GRAY + "玩家: " + ChatColor.WHITE + playerCount + "/" + maxPlayers
        ));
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 检查是否是房间列表 GUI
     */
    public static boolean isRoomListGUI(String title) {
        return title != null && title.startsWith(GUI_TITLE_PREFIX);
    }

    /**
     * 从 GUI 标题获取组名称
     */
    public static String getGroupNameFromTitle(String title) {
        if (!isRoomListGUI(title)) {
            return null;
        }
        return title.substring(GUI_TITLE_PREFIX.length()).toLowerCase();
    }
}