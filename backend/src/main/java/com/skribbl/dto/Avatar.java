package com.skribbl.dto;

public record Avatar(String face, String color) {

    public static Avatar defaultAvatar() {
        return new Avatar("🐵", "#fbbf24");
    }
}
