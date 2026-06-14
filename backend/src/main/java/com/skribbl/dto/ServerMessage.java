package com.skribbl.dto;

/**
 * Envelope for every server -&gt; client message on a room topic.
 * The client switches on {@code type} and interprets {@code data} accordingly.
 *
 * Types: PLAYERS, STATE, ROUND_START, WORD_OPTIONS, DRAW, CLEAR, UNDO, GUESS,
 * CHAT, HINT, ROUND_END, GAME_OVER, SYSTEM, ERROR, JOINED.
 */
public record ServerMessage(String type, Object data, long ts) {

    public static ServerMessage of(String type, Object data) {
        return new ServerMessage(type, data, System.currentTimeMillis());
    }
}
