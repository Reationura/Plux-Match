package com.pluxmatch.database;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;
import com.pluxmatch.model.PlayerData;
import com.pluxmatch.model.MatchRecord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bson.Document;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库管理（平台无关），支持 MySQL / SQLite / MongoDB。
 *
 * @author Reationura
 */
public class DatabaseManager {

    private final PluxMatchCore core;
    private final ConfigManager configManager;
    private final String databaseType;

    private HikariDataSource dataSource;
    private com.mongodb.client.MongoClient mongoClient;
    private com.mongodb.client.MongoDatabase mongoDatabase;

    private final ConcurrentHashMap<UUID, PlayerData> playerCache;

    public DatabaseManager(PluxMatchCore core) {
        this.core = core;
        this.configManager = core.getConfigManager();
        Map<String, Object> config = configManager.getConfig();
        this.databaseType = configManager.getString(config, "database.type", "sqlite");
        this.playerCache = new ConcurrentHashMap<>();
        connect();
    }

    private void connect() {
        switch (databaseType.toLowerCase()) {
            case "mysql":
                setupMySQL();
                break;
            case "mongodb":
                setupMongoDB();
                break;
            case "sqlite":
            default:
                setupSQLite();
        }
        initializeTables();
    }

    private void setupMySQL() {
        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> mysql = configManager.getSection(config, "database.mysql");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" +
            configManager.getString(mysql, "host", "localhost") + ":" +
            configManager.getInt(mysql, "port", 3306) + "/" +
            configManager.getString(mysql, "database", "pluxmatch") +
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        hikariConfig.setUsername(configManager.getString(mysql, "username", "root"));
        hikariConfig.setPassword(configManager.getString(mysql, "password", "password"));
        hikariConfig.setMaximumPoolSize(configManager.getInt(mysql, "pool-size", 10));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        dataSource = new HikariDataSource(hikariConfig);
    }

    private void setupSQLite() {
        java.io.File dataFolder = core.bridge().dataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> sqlite = configManager.getSection(config, "database.sqlite");
        String dbPath = dataFolder.getAbsolutePath() + "/" +
            configManager.getString(sqlite, "file", "data.db");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbPath);
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setMaximumPoolSize(5);

        dataSource = new HikariDataSource(hikariConfig);
    }

    private void setupMongoDB() {
        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> mongodb = configManager.getSection(config, "database.mongodb");

        String uri = configManager.getString(mongodb, "uri", "mongodb://localhost:27017");
        String database = configManager.getString(mongodb, "database", "pluxmatch");

        mongoClient = com.mongodb.client.MongoClients.create(uri);
        mongoDatabase = mongoClient.getDatabase(database);
    }

    private void initializeTables() {
        if ("mongodb".equalsIgnoreCase(databaseType)) {
            return;
        }

        String[] createTableSQL = {
            "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(64), " +
                "elo INTEGER DEFAULT 1000, " +
                "wins INTEGER DEFAULT 0, " +
                "losses INTEGER DEFAULT 0, " +
                "tier VARCHAR(64), " +
                "last_join_time BIGINT DEFAULT 0, " +
                "last_leave_time BIGINT DEFAULT 0, " +
                "current_queue VARCHAR(64), " +
                "current_server VARCHAR(64), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",
            "CREATE TABLE IF NOT EXISTS match_records (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "queue_id VARCHAR(64), " +
                "server_name VARCHAR(64), " +
                "start_time BIGINT, " +
                "end_time BIGINT, " +
                "result VARCHAR(32), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",
            "CREATE TABLE IF NOT EXISTS match_players (" +
                "match_id VARCHAR(36), " +
                "player_uuid VARCHAR(36), " +
                "won BOOLEAN, " +
                "elo_change INTEGER, " +
                "PRIMARY KEY (match_id, player_uuid)" +
            ")"
        };

        try (Connection conn = getConnection()) {
            for (String sql : createTableSQL) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            core.logger().severe("Failed to initialize database tables");
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("No database connection available");
        }
        return dataSource.getConnection();
    }

    private boolean isMySQL() {
        return "mysql".equalsIgnoreCase(databaseType);
    }

    public void savePlayer(PlayerData playerData) {
        playerCache.put(playerData.getUuid(), playerData);

        core.executor().submit(() -> {
            try {
                if ("mongodb".equalsIgnoreCase(databaseType)) {
                    savePlayerMongoDB(playerData);
                } else {
                    savePlayerSQL(playerData);
                }
            } catch (Exception e) {
                core.logger().severe("Failed to save player: " + playerData.getName());
                e.printStackTrace();
            }
        });
    }

    private void savePlayerSQL(PlayerData playerData) throws SQLException {
        String sql = isMySQL()
            ? "INSERT INTO players (uuid, name, elo, wins, losses, tier, last_join_time, last_leave_time, " +
              "current_queue, current_server, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
              "ON DUPLICATE KEY UPDATE name=VALUES(name), elo=VALUES(elo), wins=VALUES(wins), losses=VALUES(losses), " +
              "tier=VALUES(tier), last_join_time=VALUES(last_join_time), last_leave_time=VALUES(last_leave_time), " +
              "current_queue=VALUES(current_queue), current_server=VALUES(current_server), updated_at=CURRENT_TIMESTAMP"
            : "INSERT INTO players (uuid, name, elo, wins, losses, tier, last_join_time, last_leave_time, " +
              "current_queue, current_server, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
              "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, elo=excluded.elo, wins=excluded.wins, " +
              "losses=excluded.losses, tier=excluded.tier, last_join_time=excluded.last_join_time, " +
              "last_leave_time=excluded.last_leave_time, current_queue=excluded.current_queue, " +
              "current_server=excluded.current_server, updated_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerData.getUuid().toString());
            stmt.setString(2, playerData.getName());
            stmt.setInt(3, playerData.getElo());
            stmt.setInt(4, playerData.getWins());
            stmt.setInt(5, playerData.getLosses());
            stmt.setString(6, playerData.getTier());
            stmt.setLong(7, playerData.getLastJoinTime());
            stmt.setLong(8, playerData.getLastLeaveTime());
            stmt.setString(9, playerData.getCurrentQueue());
            stmt.setString(10, playerData.getCurrentServer());
            stmt.executeUpdate();
        }
    }

    private void savePlayerMongoDB(PlayerData playerData) {
        Document doc = new Document("_id", playerData.getUuid().toString())
            .append("name", playerData.getName())
            .append("elo", playerData.getElo())
            .append("wins", playerData.getWins())
            .append("losses", playerData.getLosses())
            .append("tier", playerData.getTier())
            .append("last_join_time", playerData.getLastJoinTime())
            .append("last_leave_time", playerData.getLastLeaveTime())
            .append("current_queue", playerData.getCurrentQueue())
            .append("current_server", playerData.getCurrentServer());

        mongoDatabase.getCollection("players").replaceOne(
            new Document("_id", playerData.getUuid().toString()),
            doc,
            new com.mongodb.client.model.ReplaceOptions().upsert(true)
        );
    }

    public PlayerData loadPlayer(UUID uuid) {
        PlayerData cached = playerCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        try {
            PlayerData playerData;
            if ("mongodb".equalsIgnoreCase(databaseType)) {
                playerData = loadPlayerMongoDB(uuid);
            } else {
                playerData = loadPlayerSQL(uuid);
            }

            if (playerData != null) {
                playerCache.put(uuid, playerData);
            }
            return playerData;
        } catch (Exception e) {
            core.logger().severe("Failed to load player: " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    private PlayerData loadPlayerSQL(UUID uuid) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM players WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        PlayerData playerData = new PlayerData(uuid, rs.getString("name"));
                        playerData.setElo(rs.getInt("elo"));
                        playerData.setWins(rs.getInt("wins"));
                        playerData.setLosses(rs.getInt("losses"));
                        playerData.setTier(readNullableString(rs, "tier"));
                        playerData.setLastJoinTime(rs.getLong("last_join_time"));
                        playerData.setLastLeaveTime(rs.getLong("last_leave_time"));
                        playerData.setCurrentQueue(rs.getString("current_queue"));
                        playerData.setCurrentServer(rs.getString("current_server"));
                        return playerData;
                    }
                }
            }
        }
        return null;
    }

    private String readNullableString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null; // 旧表尚无该列
        }
    }

    private PlayerData loadPlayerMongoDB(UUID uuid) {
        Document doc = mongoDatabase.getCollection("players")
            .find(new Document("_id", uuid.toString()))
            .first();

        if (doc == null) {
            return null;
        }

        PlayerData playerData = new PlayerData(uuid, doc.getString("name"));
        playerData.setElo(doc.getInteger("elo", 1000));
        playerData.setWins(doc.getInteger("wins", 0));
        playerData.setLosses(doc.getInteger("losses", 0));
        playerData.setTier(doc.getString("tier"));

        Object lastJoinTime = doc.get("last_join_time");
        playerData.setLastJoinTime(lastJoinTime instanceof Number ? ((Number) lastJoinTime).longValue() : 0);

        Object lastLeaveTime = doc.get("last_leave_time");
        playerData.setLastLeaveTime(lastLeaveTime instanceof Number ? ((Number) lastLeaveTime).longValue() : 0);

        playerData.setCurrentQueue(doc.getString("current_queue"));
        playerData.setCurrentServer(doc.getString("current_server"));
        return playerData;
    }

    public void saveMatchRecord(MatchRecord record) {
        core.executor().submit(() -> {
            try {
                if ("mongodb".equalsIgnoreCase(databaseType)) {
                    saveMatchRecordMongoDB(record);
                } else {
                    saveMatchRecordSQL(record);
                }
            } catch (Exception e) {
                core.logger().severe("Failed to save match record: " + record.getId());
                e.printStackTrace();
            }
        });
    }

    private void saveMatchRecordSQL(MatchRecord record) throws SQLException {
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO match_records (id, queue_id, server_name, start_time, end_time, result) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, record.getId());
                stmt.setString(2, record.getQueueId());
                stmt.setString(3, record.getServerName());
                stmt.setLong(4, record.getStartTime());
                stmt.setLong(5, record.getEndTime());
                stmt.setString(6, record.getResult().name());
                stmt.executeUpdate();
            }

            for (MatchRecord.PlayerResult pr : record.getPlayerResults()) {
                String playerSql = "INSERT INTO match_players (match_id, player_uuid, won, elo_change) " +
                    "VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(playerSql)) {
                    stmt.setString(1, record.getId());
                    stmt.setString(2, pr.getPlayerId().toString());
                    stmt.setBoolean(3, pr.isWon());
                    stmt.setInt(4, pr.getEloChange());
                    stmt.executeUpdate();
                }
            }
        }
    }

    private void saveMatchRecordMongoDB(MatchRecord record) {
        Document doc = new Document("_id", record.getId())
            .append("queue_id", record.getQueueId())
            .append("server_name", record.getServerName())
            .append("start_time", record.getStartTime())
            .append("end_time", record.getEndTime())
            .append("result", record.getResult().name());

        List<Document> playerResults = new ArrayList<>();
        for (MatchRecord.PlayerResult pr : record.getPlayerResults()) {
            playerResults.add(new Document("player_uuid", pr.getPlayerId().toString())
                .append("won", pr.isWon())
                .append("elo_change", pr.getEloChange()));
        }
        doc.append("player_results", playerResults);

        mongoDatabase.getCollection("match_records").insertOne(doc);
    }

    public List<MatchRecord> getPlayerMatchHistory(UUID playerId, int limit) {
        List<MatchRecord> records = new ArrayList<>();

        try {
            if ("mongodb".equalsIgnoreCase(databaseType)) {
                records = getPlayerMatchHistoryMongoDB(playerId, limit);
            } else {
                records = getPlayerMatchHistorySQL(playerId, limit);
            }
        } catch (Exception e) {
            core.logger().severe("Failed to get match history for player: " + playerId);
            e.printStackTrace();
        }

        return records;
    }

    private List<MatchRecord> getPlayerMatchHistorySQL(UUID playerId, int limit) throws SQLException {
        List<MatchRecord> records = new ArrayList<>();

        try (Connection conn = getConnection()) {
            String sql = "SELECT mr.* FROM match_records mr " +
                "JOIN match_players mp ON mr.id = mp.match_id " +
                "WHERE mp.player_uuid = ? " +
                "ORDER BY mr.start_time DESC LIMIT ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setInt(2, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(new MatchRecord(
                            rs.getString("id"),
                            rs.getString("queue_id"),
                            rs.getString("server_name")
                        ));
                    }
                }
            }
        }

        return records;
    }

    private List<MatchRecord> getPlayerMatchHistoryMongoDB(UUID playerId, int limit) {
        List<MatchRecord> records = new ArrayList<>();

        mongoDatabase.getCollection("match_records")
            .find(new Document("player_results.player_uuid", playerId.toString()))
            .sort(new Document("start_time", -1))
            .limit(limit)
            .forEach(doc -> records.add(new MatchRecord(
                doc.getString("_id"),
                doc.getString("queue_id"),
                doc.getString("server_name")
            )));

        return records;
    }

    /**
     * 排行榜查询（仅 SQL 后端支持；MongoDB 返回空列表）。
     *
     * @param byWinRate true 按胜率排序，false 按 ELO 排序
     * @param limit     取前 N 名
     */
    public List<LeaderboardEntry> getTopPlayers(boolean byWinRate, int limit) {
        List<LeaderboardEntry> result = new ArrayList<>();
        if ("mongodb".equalsIgnoreCase(databaseType) || dataSource == null) {
            return result;
        }

        String order = byWinRate
            ? "CAST(wins AS DOUBLE) / NULLIF(wins + losses, 0) DESC"
            : "elo DESC";
        if (isMySQL()) {
            // MySQL 支持上面的表达式
        } else {
            order = byWinRate
                ? "wins * 1.0 / MAX(wins + losses, 1) DESC"
                : "elo DESC";
        }

        String sql = "SELECT uuid, name, elo, wins, losses FROM players ORDER BY " + order + " LIMIT ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new LeaderboardEntry(
                        rs.getString("uuid"),
                        rs.getString("name"),
                        rs.getInt("elo"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                    ));
                }
            }
        } catch (SQLException e) {
            core.logger().severe("Failed to query leaderboard");
            e.printStackTrace();
        }
        return result;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
        playerCache.clear();
    }

    public void invalidateCache(UUID uuid) {
        playerCache.remove(uuid);
    }

    public String getDatabaseType() {
        return databaseType;
    }
}
