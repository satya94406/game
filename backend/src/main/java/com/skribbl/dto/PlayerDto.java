package com.skribbl.dto;

/** Wire view of a player. {@code guessed} reflects the current turn only. */
public record PlayerDto(
        String id,
        String name,
        Avatar avatar,
        int score,
        boolean host,
        boolean drawing,
        boolean guessed,
        boolean connected) {
}
