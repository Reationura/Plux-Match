package com.pluxmatch.bukkit.gui;

import com.pluxmatch.bukkit.compat.GuiIcon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * 匹配主菜单 GUI
 *
 * @author Reationura
 */
public class MatchGUI {

    private static final String GUI_TITLE_PREFIX = "匹配: ";

    /**
     * 打开指定队列组的匹配主菜单
     *
     * @param player 玩家
     * @param groupName 组名称
     */
    public static void openMatchGUI(Player player, String groupName) {
        String title = GUI_TITLE_PREFIX + groupName;
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        // 随机匹配按钮 (绿色染料)
        ItemStack randomMatch = GuiIcon.create("LIME_DYE");
        ItemMeta randomMeta = randomMatch.getItemMeta();
        randomMeta.setDisplayName(ChatColor.GREEN + "随机匹配");
        randomMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "点击加入匹配队列",
            ChatColor.GRAY + "系统将自动为您分配房间"
        ));
        randomMatch.setItemMeta(randomMeta);
        inventory.setItem(11, randomMatch);

        // 查看房间列表按钮 (指南针)
        ItemStack viewRooms = GuiIcon.create("COMPASS");
        ItemMeta roomsMeta = viewRooms.getItemMeta();
        roomsMeta.setDisplayName(ChatColor.YELLOW + "查看房间列表");
        roomsMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "查看该组的所有房间",
            ChatColor.GRAY + "包含状态和玩家数量"
        ));
        viewRooms.setItemMeta(roomsMeta);
        inventory.setItem(13, viewRooms);

        // 取消按钮 (屏障)
        ItemStack cancel = GuiIcon.create("BARRIER");
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "关闭菜单");
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(15, cancel);

        // 填充空白槽位 (灰色玻璃)
        ItemStack filler = GuiIcon.create("GRAY_STAINED_GLASS_PANE");
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    /**
     * 检查是否是匹配 GUI
     */
    public static boolean isMatchGUI(String title) {
        return title != null && title.startsWith(GUI_TITLE_PREFIX);
    }

    /**
     * 从 GUI 标题获取组名称
     */
    public static String getGroupNameFromTitle(String title) {
        if (!isMatchGUI(title)) {
            return null;
        }
        return title.substring(GUI_TITLE_PREFIX.length()).toLowerCase();
    }
}