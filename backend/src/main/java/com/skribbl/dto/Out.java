package com.skribbl.dto;

import com.skribbl.game.RoomSettings;

import java.util.List;


public final class Out {

    private Out() {
    }

    public record Joined(
            String playerId,
            String code,
            RoomSettings settings,
            List<PlayerDto> players,
            String hostId,
            String phase) {
    }

    public record Players(List<PlayerDto> players, String hostId) {
    }

    public record State(
            String phase,
            int round,
            int totalRounds,
            String drawerId,
            String drawerName,
            String maskedWord,
            int wordLength,
            int timeLeft,
            int totalTime,
            List<PlayerDto> players,
            String hostId,
            RoomSettings settings,
            List<ServerMessage> replay) {
    }

    public record RoundStart(
            int round,
            int totalRounds,
            String drawerId,
            String drawerName,
            int chooseTime) {
    }

    public record WordOptions(List<String> words, int chooseTime) {
    }

    public record Hint(String maskedWord) {
    }

    public record Guess(String playerId, String playerName, boolean correct, String text) {
    }

    public record Chat(String playerId, String playerName, String text) {
    }

    public record ScoreRow(String playerId, String playerName, int total, int gained) {
    }

    public record RoundEnd(String word, List<ScoreRow> scores, String nextDrawerId, String reason) {
    }

    public record GameOver(List<PlayerDto> leaderboard, String winnerId, String winnerName) {
    }

    public record System(String text) {
    }

    public record Error(String message) {
    }
}
