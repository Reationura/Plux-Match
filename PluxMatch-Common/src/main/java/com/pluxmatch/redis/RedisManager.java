package com.pluxmatch.redis;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.event.CrossProxyMessage;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;

/**
 * Redis 管理（平台无关），用于跨网关状态同步。
 * 收到的消息以 {@link CrossProxyMessage} 发布到核心事件总线，由订阅方处理真正的同步逻辑。
 *
 * @author Reationura
 */
public class RedisManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final String channel;

    private JedisPool jedisPool;
    private Thread subscriberThread;

    public RedisManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();

        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> redisConfig = configManager.getSection(config, "redis");

        String host = configManager.getString(redisConfig, "host", "localhost");
        int port = configManager.getInt(redisConfig, "port", 6379);
        String password = configManager.getString(redisConfig, "password", "");
        this.channel = configManager.getString(redisConfig, "channel", "pluxmatch");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);

        if (password == null || password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, Protocol.DEFAULT_TIMEOUT, password);
        }

        startSubscriber();
        core.logger().info("Redis connection established");
    }

    private void startSubscriber() {
        subscriberThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String ch, String message) {
                        handleMessage(message);
                    }
                }, channel);
            } catch (Exception e) {
                core.logger().severe("Redis subscriber error");
                e.printStackTrace();
            }
        }, "PluxMatch-Redis-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void handleMessage(String message) {
        if (message == null) {
            return;
        }
        int sep = message.indexOf('|');
        if (sep < 0 || sep == message.length() - 1) {
            return;
        }
        String action = message.substring(0, sep);
        String data = message.substring(sep + 1);
        // 发布到事件总线，交由订阅方（核心/第三方）处理真正的跨代理同步
        core.eventBus().publish(new CrossProxyMessage(action, data));
    }

    public void publish(String action, String data) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, action + "|" + data);
        } catch (Exception e) {
            core.logger().warning("Failed to publish redis message: " + e.getMessage());
        }
    }

    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
        }
    }

    public void set(String key, String value, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, seconds, value);
        }
    }

    public void del(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        }
    }

    public void close() {
        if (subscriberThread != null && subscriberThread.isAlive()) {
            subscriberThread.interrupt();
        }
        if (jedisPool != null) {
            jedisPool.close();
        }
        core.logger().info("Redis connection closed");
    }
}
