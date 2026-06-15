package com.skribbl.dto;

import com.skribbl.game.RoomSettings;

import java.util.List;


public final class In {

    private In() {
    }

    public record CreateRoom(RoomSettings settings) {
    }

    public record Join(String playerId, String name, Avatar avatar) {
    }

    public record UpdateSettings(RoomSettings settings) {
    }

    public record WordChosen(String word) {
    }

    public record Draw(
            String strokeId,
            String color,
            double size,
            boolean eraser,
            List<PointDto> points,
            boolean done) {
    }

    public record Guess(String text) {
    }

    public record Chat(String text) {
    }
}
