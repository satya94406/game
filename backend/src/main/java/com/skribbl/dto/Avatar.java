package com.skribbl.dto;

/** A player's avatar: an emoji face plus a background color (hex). */
public record Avatar(String face, String color) {

    public static Avatar defaultAvatar() {
        return new Avatar("🐵", "#fbbf24");
    }
}
