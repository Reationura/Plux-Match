package com.pluxmatch.tier;

/**
 * 段位定义。
 *
 * @author Reationura
 */
public class Tier {

    private final String name;
    private final String displayName; // 含颜色代码，可为 null
    private final int minElo;

    public Tier(String name, String displayName, int minElo) {
        this.name = name;
        this.displayName = displayName;
        this.minElo = minElo;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public int getMinElo() {
        return minElo;
    }
}
