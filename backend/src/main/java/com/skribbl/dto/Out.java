package com.skribbl.dto;

import com.skribbl.game.RoomSettings;

import java.util.List;

/**
 * Outbound payloads — each is the {@code data} of a {@link ServerMessage}.
 * The {@code type} string on the envelope tells the client which record this is.
 */
public final class Out {

    private Out() {
    }

    /** Sent privately to a player right after they join, carrying the room snapshot. */
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

    /** Full snapshot for the current phase; {@code replay} carries the canvas for late joiners. */
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

    /** Sent privately to the drawer only. */
    public record WordOptions(List<String> words, int chooseTime) {
    }

    public record Hint(String maskedWord) {
    }

    /** {@code text} is present only for incorrect guesses (shown as a chat line). */
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
