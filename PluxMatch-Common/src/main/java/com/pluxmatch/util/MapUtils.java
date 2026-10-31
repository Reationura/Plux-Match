package com.pluxmatch.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map 构建工具，用于消息占位符替换。
 *
 * @author Reationura
 */
public class MapUtils {

    public static Map<String, String> of(String k1, String v1) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static Map<String, String> of(String k1, String v1, String k2, String v2) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, String> of(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
