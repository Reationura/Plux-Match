package com.pluxmatch.bukkit.compat;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * GUI 物品材质工厂（跨 1.7 → 最新）。
 * <p>
 * 通过 {@link Material#matchMaterial(String)} 以字符串解析现代扁平化材质名：
 * 现代服务端（{@code >=1.13}）直接命中；旧服务端（{@code 1.7-1.12}）返回
 * {@code null}，此时按 {@link #LEGACY_FALLBACK} 回退到旧材质名 + durability 数据。
 * 全程不直接引用 {@code Material.LIME_DYE} 等现代枚举常量，避免旧 API 编译/加载期
 * {@code NoSuchFieldError}。
 *
 * @author Reationura
 */
public final class GuiIcon {

    /** 现代名 → 旧端回退（旧材质名, durability 数据）。 */
    private static final Map<String, String[]> LEGACY_FALLBACK = new HashMap<String, String[]>();

    static {
        LEGACY_FALLBACK.put("LIME_DYE", new String[]{"INK_SACK", "10"});
        LEGACY_FALLBACK.put("GRAY_STAINED_GLASS_PANE", new String[]{"STAINED_GLASS_PANE", "7"});
        LEGACY_FALLBACK.put("BARRIER", new String[]{"BEDROCK", "0"});
        LEGACY_FALLBACK.put("LIME_WOOL", new String[]{"WOOL", "5"});
        LEGACY_FALLBACK.put("YELLOW_WOOL", new String[]{"WOOL", "4"});
        LEGACY_FALLBACK.put("ORANGE_WOOL", new String[]{"WOOL", "1"});
        LEGACY_FALLBACK.put("RED_WOOL", new String[]{"WOOL", "14"});
        LEGACY_FALLBACK.put("GRAY_WOOL", new String[]{"WOOL", "7"});
    }

    private GuiIcon() {
    }

    /**
     * 按现代材质名创建物品；旧服务端自动回退到旧名 + 数据。
     *
     * @param modernName 现代扁平化材质名（如 {@code LIME_WOOL}）
     * @return 对应 {@link ItemStack}；极端情况下（无任何可用材质）返回 {@code null}
     */
    public static ItemStack create(String modernName) {
        Material material = matchMaterialSafe(modernName);
        if (material != null) {
            return new ItemStack(material);
        }
        String[] fallback = LEGACY_FALLBACK.get(modernName);
        if (fallback != null) {
            Material legacy = matchMaterialSafe(fallback[0]);
            if (legacy != null) {
                ItemStack item = new ItemStack(legacy);
                item.setDurability(Short.parseShort(fallback[1]));
                return item;
            }
        }
        // 最后兜底：全版本存在的石头，避免 GUI 出现 null/AIR
        Material stone = matchMaterialSafe("STONE");
        return stone != null ? new ItemStack(stone) : null;
    }

    private static Material matchMaterialSafe(String name) {
        try {
            return Material.matchMaterial(name);
        } catch (Throwable t) {
            return null;
        }
    }
}
