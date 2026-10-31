package com.pluxmatch.tier;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.event.TierPromotionEvent;
import com.pluxmatch.model.PlayerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 段位管理（平台无关）：按 ELO 阈值映射段位，并在升/降段时发布事件。
 *
 * @author Reationura
 */
public class TierManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final List<Tier> tiers = new ArrayList<>();

    public TierManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        loadTiers();
    }

    @SuppressWarnings("unchecked")
    private void loadTiers() {
        tiers.clear();
        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> tierSection = configManager.getSection(config, "tiers");

        for (Map.Entry<String, Object> entry : tierSection.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> data = (Map<String, Object>) entry.getValue();
            int minElo = configManager.getInt(data, "min-elo", 0);
            String display = configManager.getString(data, "display", null);
            tiers.add(new Tier(entry.getKey(), display, minElo));
        }

        tiers.sort(Comparator.comparingInt(Tier::getMinElo));
        core.logger().info("Loaded " + tiers.size() + " tiers");
    }

    public void reload() {
        loadTiers();
    }

    /**
     * 依 ELO 返回对应段位，无配置时返回 null。
     */
    public Tier getTier(int elo) {
        Tier result = null;
        for (Tier tier : tiers) {
            if (elo >= tier.getMinElo()) {
                result = tier;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * 依据玩家当前 ELO 刷新段位，若发生变化则更新数据并发布 {@link TierPromotionEvent}。
     *
     * @return 是否发生段位变化
     */
    public boolean updatePlayerTier(PlayerData data) {
        if (tiers.isEmpty() || data == null) {
            return false;
        }
        Tier tier = getTier(data.getElo());
        String newName = tier != null ? tier.getName() : null;
        String oldName = data.getTier();

        if (newName != null && !newName.equals(oldName)) {
            data.setTier(newName);
            core.getDatabaseManager().savePlayer(data);
            core.eventBus().publish(new TierPromotionEvent(data.getUuid(), oldName, newName));
            return true;
        }
        return false;
    }

    /**
     * 获取段位显示名（含颜色），无段位返回 "&7未定级"。
     */
    public String getDisplayName(String tierName) {
        if (tierName == null) {
            return "&7未定级";
        }
        for (Tier tier : tiers) {
            if (tier.getName().equals(tierName)) {
                return tier.getDisplayName();
            }
        }
        return tierName;
    }

    public List<Tier> getTiers() {
        return new ArrayList<>(tiers);
    }
}
