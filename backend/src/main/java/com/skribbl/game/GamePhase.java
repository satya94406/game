package com.skribbl.game;

/**
 * Lifecycle of a room/game.
 *
 * <pre>
 * LOBBY -> CHOOSING -> DRAWING -> ROUND_END -> (CHOOSING ...) -> GAME_OVER
 * </pre>
 */
public enum GamePhase {
    LOBBY,
    CHOOSING,
    DRAWING,
    ROUND_END,
    GAME_OVER
}
