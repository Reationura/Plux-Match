package com.pluxmatch.elo;

import com.pluxmatch.PluxMatchCore;
import com.pluxmatch.config.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ELO 计分管理（平台无关）。
 *
 * @author Reationura
 */
public class ELOManager {

    private final ConfigManager configManager;

    private final int initialElo;
    private final int kFactor;
    private final int minKFactor;
    private final int maxKFactor;

    public ELOManager(PluxMatchCore core) {
        this.configManager = core.getConfigManager();

        Map<String, Object> config = configManager.getConfig();
        Map<String, Object> eloConfig = configManager.getSection(config, "matchmaking.elo");

        this.initialElo = configManager.getInt(eloConfig, "initial", 1000);
        this.kFactor = configManager.getInt(eloConfig, "k-factor", 32);
        this.minKFactor = configManager.getInt(eloConfig, "min-k-factor", 16);
        this.maxKFactor = configManager.getInt(eloConfig, "max-k-factor", 48);
    }

    public int calculateNewElo(int currentElo, int opponentElo, boolean won) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentElo - currentElo) / 400.0));
        int actualScore = won ? 1 : 0;

        int k = getKFactor(currentElo);
        int eloChange = (int) Math.round(k * (actualScore - expectedScore));

        return currentElo + eloChange;
    }

    public int[] calculateMatchElos(int[] team1Elos, int[] team2Elos, boolean team1Won) {
        int team1Average = team1Elos.length > 0 ?
            (int) Math.round(java.util.Arrays.stream(team1Elos).average().orElse(initialElo)) : initialElo;
        int team2Average = team2Elos.length > 0 ?
            (int) Math.round(java.util.Arrays.stream(team2Elos).average().orElse(initialElo)) : initialElo;

        int[] results = new int[team1Elos.length + team2Elos.length];

        for (int i = 0; i < team1Elos.length; i++) {
            results[i] = calculateNewElo(team1Elos[i], team2Average, team1Won);
        }

        for (int i = 0; i < team2Elos.length; i++) {
            results[team1Elos.length + i] = calculateNewElo(team2Elos[i], team1Average, !team1Won);
        }

        return results;
    }

    public Map<UUID, Integer> calculateMatchElos(Map<UUID, Integer> team1, Map<UUID, Integer> team2, boolean team1Won) {
        Map<UUID, Integer> results = new HashMap<>();

        int team1Average = (int) team1.values().stream().mapToInt(Integer::intValue).average().orElse(initialElo);
        int team2Average = (int) team2.values().stream().mapToInt(Integer::intValue).average().orElse(initialElo);

        for (Map.Entry<UUID, Integer> entry : team1.entrySet()) {
            results.put(entry.getKey(), calculateNewElo(entry.getValue(), team2Average, team1Won));
        }

        for (Map.Entry<UUID, Integer> entry : team2.entrySet()) {
            results.put(entry.getKey(), calculateNewElo(entry.getValue(), team1Average, !team1Won));
        }

        return results;
    }

    public int getKFactor(int elo) {
        if (elo < 1000) {
            return maxKFactor;
        } else if (elo > 2000) {
            return minKFactor;
        }

        double ratio = (elo - 1000) / 1000.0;
        return minKFactor + (int) ((maxKFactor - minKFactor) * (1 - ratio));
    }

    public double calculateGlickoRating(double rating, double rd, double[] opponentRatings,
                                         double[] opponentRDs, boolean[] results) {
        double q = Math.log(10) / 400;
        double d2 = 0;

        for (int i = 0; i < opponentRatings.length; i++) {
            double g = 1 / Math.sqrt(1 + (3 * q * q * opponentRDs[i] * opponentRDs[i]) / (Math.PI * Math.PI));
            double e = 1 / (1 + Math.pow(10, (-g * (rating - opponentRatings[i])) / 400));
            d2 += g * g * e * (1 - e);
        }

        d2 = 1 / (q * q * d2);

        double delta = 0;
        for (int i = 0; i < opponentRatings.length; i++) {
            double g = 1 / Math.sqrt(1 + (3 * q * q * opponentRDs[i] * opponentRDs[i]) / (Math.PI * Math.PI));
            double e = 1 / (1 + Math.pow(10, (-g * (rating - opponentRatings[i])) / 400));
            double s = results[i] ? 1 : 0;
            delta += g * (s - e);
        }
        delta *= q * d2 / 400;

        return rating + delta;
    }

    public double calculateTrueSkill(double[] team1Skills, double[] team2Skills, boolean team1Won) {
        double team1Avg = java.util.Arrays.stream(team1Skills).average().orElse(25.0);
        double team2Avg = java.util.Arrays.stream(team2Skills).average().orElse(25.0);

        double delta = team1Avg - team2Avg;
        double prob = 1 / (1 + Math.exp(-delta / 4));

        return team1Won ? prob : 1 - prob;
    }

    public int getInitialElo() {
        return initialElo;
    }

    public int getKFactor() {
        return kFactor;
    }

    public int calculateEloVariance(int[] elos) {
        if (elos.length == 0) return 0;

        double mean = java.util.Arrays.stream(elos).average().orElse(0);
        double variance = java.util.Arrays.stream(elos)
            .mapToDouble(e -> Math.pow(e - mean, 2))
            .average()
            .orElse(0);

        return (int) Math.sqrt(variance);
    }

    public int calculateTeamAverageElo(List<Integer> elos) {
        if (elos == null || elos.isEmpty()) {
            return initialElo;
        }
        return (int) Math.round(elos.stream().mapToInt(Integer::intValue).average().orElse(initialElo));
    }
}
