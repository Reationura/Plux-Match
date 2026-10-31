package com.pluxmatch.guis;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.model.MatchRecord;
import com.pluxmatch.model.Party;
import com.pluxmatch.model.Queue;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.util.ByteBufWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 菜单管理（平台无关）。
 * <ul>
 *   <li>聊天式菜单使用 Adventure {@link Component}（点击/悬停事件），跨 Bungee/Velocity 通用；</li>
 *   <li>箱子 GUI 仍经 {@link PlatformBridge#sendData} 交由 Bukkit 端渲染（保持现状）。</li>
 * </ul>
 *
 * @author Reationura
 */
public class GUIManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final PlatformBridge bridge;

    public GUIManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        this.bridge = core.bridge();
    }

    /**
     * 打开指定队列组的箱子 GUI：Bungee/Velocity 均无原生箱子界面，故经 Plugin Message 请求 Bukkit 端打开。
     */
    public void openQueueGroupGUI(MatchPlayer player, String groupName) {
        if (player.currentServerName() == null) {
            bridge.sendMessage(player, legacy("&c错误：你不在任何服务器上！"));
            return;
        }

        byte[] payload = ByteBufWriter.create()
            .utf("OpenGUI").utf(player.getName()).utf(groupName)
            .toByteArray();
        bridge.sendData(player, "pluxmatch:response", payload);
    }

    public void openMainMenu(MatchPlayer player) {
        Map<String, Object> guiConfig = configManager.getGui();
        Map<String, Object> menuConfig = configManager.getSection(guiConfig, "main-menu");

        String title = configManager.getString(menuConfig, "title", "&6匹配菜单");

        Component menu = Component.empty()
            .append(legacy("\n" + title + "\n\n"));

        for (Queue queue : core.getQueueManager().getEnabledQueues()) {
            if (!queue.isEnabled()) {
                continue;
            }

            String itemName = configManager.getString(menuConfig, "items.queue-item.name", "&6{queue_name}")
                .replace("{queue_name}", queue.getName());
            String status = "&a启用";

            Component lore = legacy(
                "    &7人数: " + queue.getCurrentSize() + "/" + queue.getMaxPlayers() + "\n"
                    + "    " + status + "\n"
                    + "    &7点击加入\n\n");

            Component item = legacy("  " + itemName + "\n")
                .append(lore)
                .clickEvent(ClickEvent.runCommand("/match join " + queue.getId()))
                .hoverEvent(HoverEvent.showText(legacy("点击加入 " + queue.getName())));

            menu = menu.append(item);
        }

        menu = menu.append(legacy("\n&c关闭菜单"));
        bridge.sendMessage(player, menu);
    }

    public void openPartyMenu(MatchPlayer player) {
        Map<String, Object> guiConfig = configManager.getGui();
        Map<String, Object> menuConfig = configManager.getSection(guiConfig, "party-menu");
        String title = configManager.getString(menuConfig, "title", "&6队伍管理");

        Component menu = Component.empty().append(legacy("\n" + title + "\n\n"));

        Party party = core.getPartyManager().getParty(player.getUniqueId());

        if (party == null) {
            menu = menu
                .append(legacy("&c你不在队伍中\n"))
                .append(legacy("&a点击创建队伍\n"))
                .append(legacy("  [创建队伍]")
                    .clickEvent(ClickEvent.runCommand("/match party create")));
            bridge.sendMessage(player, menu);
            return;
        }

        for (UUID member : party.getMembers()) {
            MatchPlayer memberPlayer = bridge.player(member).orElse(null);
            String name = memberPlayer != null ? memberPlayer.getName() : "离线";
            String role = party.isLeader(member) ? "&c队长" : "&a队员";
            String status = memberPlayer != null ? "&a在线" : "&c离线";

            menu = menu.append(legacy("  " + name + " - " + role + " (" + status + ")\n"));
        }

        menu = menu.append(legacy("\n"));

        if (party.isLeader(player.getUniqueId())) {
            menu = menu
                .append(legacy("[邀请玩家] ")
                    .clickEvent(ClickEvent.suggestCommand("/match party invite ")))
                .append(legacy("[踢出成员] ")
                    .clickEvent(ClickEvent.suggestCommand("/match party kick ")))
                .append(legacy("[解散队伍] ")
                    .clickEvent(ClickEvent.runCommand("/match party disband")))
                .append(legacy("\n"))
                .append(legacy("[加入队列]")
                    .clickEvent(ClickEvent.runCommand("/match")));
        }

        bridge.sendMessage(player, menu);
    }

    public void openMatchStatus(MatchPlayer player) {
        Map<String, Object> guiConfig = configManager.getGui();
        Map<String, Object> menuConfig = configManager.getSection(guiConfig, "match-status");
        String title = configManager.getString(menuConfig, "title", "&6匹配状态");

        Component menu = Component.empty().append(legacy("\n" + title + "\n\n"));

        String queueId = core.getQueueManager().getPlayerQueue(player.getUniqueId());
        if (queueId == null) {
            bridge.sendMessage(player, menu.append(legacy("&c你不在队列中")));
            return;
        }

        Queue queue = core.getQueueManager().getQueue(queueId);
        if (queue == null) {
            bridge.sendMessage(player, menu.append(legacy("&c队列不存在")));
            return;
        }

        int position = core.getQueueManager().getQueuePosition(player.getUniqueId());
        int total = queue.getCurrentSize();

        menu = menu.append(legacy(
            "&6排队中\n"
                + "&7队列: " + queue.getName() + "\n"
                + "&7位置: " + position + "/" + total + "\n"))
            .append(legacy("[退出队列]")
                .clickEvent(ClickEvent.runCommand("/match leave")));

        bridge.sendMessage(player, menu);
    }

    public void openHistoryMenu(MatchPlayer player) {
        Map<String, Object> guiConfig = configManager.getGui();
        Map<String, Object> menuConfig = configManager.getSection(guiConfig, "history-menu");
        String title = configManager.getString(menuConfig, "title", "&6历史记录");

        Component menu = Component.empty().append(legacy("\n" + title + "\n\n"));

        List<MatchRecord> records = core.getDatabaseManager().getPlayerMatchHistory(player.getUniqueId(), 10);
        if (records.isEmpty()) {
            bridge.sendMessage(player, menu.append(legacy("&c暂无对局记录")));
            return;
        }

        for (MatchRecord record : records) {
            String dateStr = new java.util.Date(record.getStartTime()).toString();
            menu = menu.append(legacy(
                "&6" + dateStr + "\n"
                    + "&7模式: " + record.getQueueId() + "\n"
                    + "&7结果: " + record.getResult() + "\n\n"));
        }

        bridge.sendMessage(player, menu);
    }

    private Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
