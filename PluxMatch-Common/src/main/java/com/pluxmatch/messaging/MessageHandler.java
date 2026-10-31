package com.pluxmatch.messaging;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.platform.MatchPlayer;
import com.pluxmatch.platform.PlatformBridge;
import com.pluxmatch.util.ByteBufWriter;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 入站 Plugin Message 处理（平台无关）。Bungee / Velocity 监听器仅解码通道并转调本类，
 * 使跨服命令转发、房间列表、GUI 打开、结果上报在两端行为一致。编码不依赖 Guava。
 *
 * @author Reationura
 */
public class MessageHandler {

    public static final String DATA_CHANNEL = "matchmaking:data";
    public static final String COMMAND_CHANNEL = "pluxmatch:command";
    public static final String RESPONSE_CHANNEL = "pluxmatch:response";

    private final PluxMatchCore core;
    private final PlatformBridge bridge;

    public MessageHandler(PluxMatchCore core) {
        this.core = core;
        this.bridge = core.bridge();
    }

    public void handle(String channel, MatchPlayer sender, byte[] data) {
        if (COMMAND_CHANNEL.equals(channel)) {
            handleCommand(data);
            return;
        }
        if (DATA_CHANNEL.equals(channel)) {
            handleData(data);
        }
    }

    private void handleData(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = in.readUTF();
            switch (action) {
                case "MATCH_RESULT": {
                    String matchId = in.readUTF();
                    int winnerCount = in.readInt();
                    List<UUID> winners = new ArrayList<>();
                    for (int i = 0; i < winnerCount; i++) {
                        winners.add(UUID.fromString(in.readUTF()));
                    }
                    int loserCount = in.readInt();
                    List<UUID> losers = new ArrayList<>();
                    for (int i = 0; i < loserCount; i++) {
                        losers.add(UUID.fromString(in.readUTF()));
                    }
                    core.getMatchManager().finishMatch(matchId, winners, losers);
                    core.logger().info("Match result received: " + matchId
                        + " - Winners: " + winnerCount + ", Losers: " + loserCount);
                    break;
                }
                case "PLAYER_JOINED": {
                    UUID playerId = UUID.fromString(in.readUTF());
                    String serverName = in.readUTF();
                    core.logger().info("Player " + playerId + " joined server: " + serverName);
                    break;
                }
                case "PLAYER_LEFT": {
                    UUID playerId = UUID.fromString(in.readUTF());
                    String serverName = in.readUTF();
                    core.logger().info("Player " + playerId + " left server: " + serverName);
                    break;
                }
                case "SERVER_READY": {
                    String serverName = in.readUTF();
                    core.logger().info("Server ready: " + serverName);
                    break;
                }
                default:
                    break;
            }
        } catch (IOException e) {
            core.logger().severe("Failed to read plugin message: " + e.getMessage());
        }
    }

    private void handleCommand(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String action = in.readUTF();
            switch (action) {
                case "ExecuteCommand": {
                    UUID playerId = UUID.fromString(in.readUTF());
                    String playerName = in.readUTF();
                    String command = in.readUTF();
                    MatchPlayer player = bridge.player(playerId).orElse(null);
                    if (player == null) {
                        core.logger().warning("Player not found for command execution: " + playerId);
                        return;
                    }
                    bridge.dispatchCommand(player, command);
                    core.logger().info("Forwarded command from Bukkit: /" + command + " by " + playerName);
                    break;
                }
                case "GetRoomList": {
                    String playerName = in.readUTF();
                    String groupName = in.readUTF();
                    MatchPlayer player = bridge.player(playerName).orElse(null);
                    if (player == null || player.currentServerName() == null) {
                        core.logger().warning("Player not found for room list: " + playerName);
                        return;
                    }
                    List<String> rooms = core.getServerManager().getRoomsByGroup(groupName);
                    ByteBufWriter out = ByteBufWriter.create().utf("RoomList").utf(playerName).utf(groupName).i(rooms.size());
                    for (String room : rooms) {
                        out.utf(room);
                    }
                    bridge.sendData(player, RESPONSE_CHANNEL, out.toByteArray());
                    break;
                }
                case "OpenGUI": {
                    String playerName = in.readUTF();
                    String groupName = in.readUTF();
                    MatchPlayer player = bridge.player(playerName).orElse(null);
                    if (player == null) {
                        core.logger().warning("Player not found for GUI: " + playerName);
                        return;
                    }
                    core.getGuiManager().openQueueGroupGUI(player, groupName);
                    break;
                }
                default:
                    break;
            }
        } catch (IOException e) {
            core.logger().severe("Failed to read command forward message: " + e.getMessage());
        }
    }
}
