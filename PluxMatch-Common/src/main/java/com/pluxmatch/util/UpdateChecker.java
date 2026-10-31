package com.pluxmatch.util;

import com.pluxmatch.PluxMatchCore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * 更新检查（平台无关）。
 *
 * @author Reationura
 */
public class UpdateChecker {

    private static final String UPDATE_URL =
        "https://api.spigotmc.org/legacy/update.php?resource=123456";

    private final PluxMatchCore core;
    private final String currentVersion;

    public UpdateChecker(PluxMatchCore core, String currentVersion) {
        this.core = core;
        this.currentVersion = currentVersion;
    }

    public void check() {
        core.executor().submit(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    String latestVersion = reader.readLine();
                    if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                        core.logger().info("========================================");
                        core.logger().info("PluxMatch update available!");
                        core.logger().info("Current version: " + currentVersion);
                        core.logger().info("Latest version: " + latestVersion);
                        core.logger().info("========================================");
                    }
                }
            } catch (Exception e) {
                core.logger().info("Failed to check for updates");
            }
        });
    }
}
