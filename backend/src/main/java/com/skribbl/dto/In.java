package com.skribbl.dto;

import com.skribbl.game.RoomSettings;

import java.util.List;

/**
 * Inbound (client -&gt; server) message payloads.
 *
 * <p>{@code playerId} is only sent on {@link Join} (the client generates it).
 * Every later action reads the sender's id from the STOMP session attributes
 * that {@code join} stored, so clients can't act on behalf of others.
 */
public final class In {

    private In() {
    }

    /** REST body for {@code POST /api/rooms}. {@code settings} may be null (defaults applied). */
    public record CreateRoom(RoomSettings settings) {
    }

    public record Join(String playerId, String name, Avatar avatar) {
    }

    public record UpdateSettings(RoomSettings settings) {
    }

    public record WordChosen(String word) {
    }

    /** A batch of new points for one stroke. Coordinates are normalized [0,1]. */
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
