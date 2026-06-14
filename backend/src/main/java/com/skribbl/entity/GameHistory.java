package com.skribbl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A finished game, persisted when a match ends. Powers the "recent games" view
 * and gives the otherwise in-memory game a durable record in MySQL.
 */
@Entity
@Table(name = "game_history")
public class GameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String roomCode;

    private int rounds;

    private int playerCount;

    @Column(length = 64)
    private String winnerName;

    private int winnerScore;

    /** JSON array of {name, score} for the full final leaderboard. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String leaderboardJson;

    @Column(nullable = false)
    private Instant finishedAt = Instant.now();

    public GameHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public int getWinnerScore() {
        return winnerScore;
    }

    public void setWinnerScore(int winnerScore) {
        this.winnerScore = winnerScore;
    }

    public String getLeaderboardJson() {
        return leaderboardJson;
    }

    public void setLeaderboardJson(String leaderboardJson) {
        this.leaderboardJson = leaderboardJson;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
