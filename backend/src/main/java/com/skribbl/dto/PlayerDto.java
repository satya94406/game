package com.skribbl.dto;

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
