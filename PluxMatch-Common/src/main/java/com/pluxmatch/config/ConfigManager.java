package com.pluxmatch.config;

import com.pluxmatch.platform.PlatformBridge;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理（平台无关）。默认配置文件从 Common 的 classpath 资源释放到数据目录。
 *
 * @author Reationura
 */
public class ConfigManager {

    private final PlatformBridge bridge;
    private final Yaml yaml;
    private Map<String, Object> config;
    private Map<String, Object> messages;
    private Map<String, Object> gui;
    private Map<String, Object> queues;
    private Map<String, Object> servers;

    public ConfigManager(PlatformBridge bridge) {
        this.bridge = bridge;
        this.yaml = new Yaml();
        loadAllConfigs();
    }

    private void loadAllConfigs() {
        File dataFolder = bridge.dataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        config = loadConfig("config.yml");
        messages = loadConfig("messages.yml");
        gui = loadConfig("gui.yml");
        queues = loadConfig("queues.yml");
        servers = loadConfig("servers.yml");
    }

    private Map<String, Object> loadConfig(String fileName) {
        File configFile = new File(bridge.dataFolder(), fileName);

        if (!configFile.exists()) {
            try (InputStream is = getDefaultResource(fileName)) {
                if (is != null) {
                    try (OutputStream os = new FileOutputStream(configFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                bridge.logger().severe("Failed to save default config: " + fileName);
                e.printStackTrace();
            }
        }

        try (InputStream is = new FileInputStream(configFile)) {
            return yaml.load(is);
        } catch (IOException e) {
            bridge.logger().severe("Failed to load config: " + fileName);
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 从打包进 Common jar 的资源读取默认配置。
     */
    private InputStream getDefaultResource(String fileName) {
        InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(fileName);
        if (is == null) {
            is = ConfigManager.class.getResourceAsStream("/" + fileName);
        }
        return is;
    }

    public void reload() {
        loadAllConfigs();
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public Map<String, Object> getMessages() {
        return messages;
    }

    public Map<String, Object> getGui() {
        return gui;
    }

    public Map<String, Object> getQueues() {
        return queues;
    }

    public Map<String, Object> getServers() {
        return servers;
    }

    public String getString(Map<String, Object> config, String path, String defaultValue) {
        Object value = getValue(config, path);
        return value != null ? value.toString() : defaultValue;
    }

    public int getInt(Map<String, Object> config, String path, int defaultValue) {
        Object value = getValue(config, path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(Map<String, Object> config, String path, boolean defaultValue) {
        Object value = getValue(config, path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    public double getDouble(Map<String, Object> config, String path, double defaultValue) {
        Object value = getValue(config, path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public List<String> getStringList(Map<String, Object> config, String path) {
        Object value = getValue(config, path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(Map<String, Object> config, String path) {
        Object value = getValue(config, path);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Object getValue(Map<String, Object> config, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return null;
            }
        }

        return current.get(parts[parts.length - 1]);
    }
}
