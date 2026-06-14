package com.skribbl.game;

import com.skribbl.dto.Avatar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests for the game engine — no Spring, no database. */
class GameTest {

    private Room roomWith(int players, int rounds) {
        RoomSettings settings = new RoomSettings();
        settings.setRounds(rounds);
        settings.setDrawTime(80);
        settings.setHints(2);
        Room room = new Room("TEST1", settings);
        for (int i = 0; i < players; i++) {
            room.addPlayer("p" + i, "Player" + i, Avatar.defaultAvatar());
        }
        return room;
    }

    @Test
    void normalizeTrimsAndLowercases() {
        assertEquals("hello world", Game.normalize("  Hello   World "));
        assertEquals("", Game.normalize(null));
    }

    @Test
    void closeGuessDetectsSingleEdit() {
        assertTrue(Game.isClose("aple", "apple"));    // one deletion
        assertTrue(Game.isClose("apples", "apple"));  // one insertion
        assertFalse(Game.isClose("orange", "apple")); // far off
        assertFalse(Game.isClose("ca", "cat"));       // word too short to hint
    }

    @Test
    void correctGuessScoresAndDrawerCannotGuess() {
        Room room = roomWith(2, 2);
        Game game = new Game(room);
        game.start();
        game.setWordOptions(List.of("apple"));
        assertTrue(game.chooseWord("apple"));
        assertEquals(GamePhase.DRAWING, game.getPhase());

        String drawer = game.getCurrentDrawerId();
        String guesser = drawer.equals("p0") ? "p1" : "p0";

        assertEquals(Game.GuessOutcome.IGNORED, game.registerGuess(drawer, "apple"));
        assertEquals(Game.GuessOutcome.WRONG, game.registerGuess(guesser, "banana"));
        assertEquals(Game.GuessOutcome.CORRECT, game.registerGuess(guesser, "APPLE")); // case-insensitive
        assertTrue(room.getPlayer(guesser).getScore() > 0);
        assertEquals(Game.GuessOutcome.IGNORED, game.registerGuess(guesser, "apple")); // already guessed
        assertTrue(game.everyoneGuessed());

        game.endTurn();
        assertEquals(GamePhase.ROUND_END, game.getPhase());
        assertTrue(room.getPlayer(drawer).getScore() > 0, "drawer earns points when others guess");
    }

    @Test
    void maskedWordHidesLettersButShowsSpaces() {
        Room room = roomWith(2, 2);
        Game game = new Game(room);
        game.start();
        game.setWordOptions(List.of("ice cream"));
        game.chooseWord("ice cream");

        String masked = game.maskedWord();
        assertEquals("ice cream".length(), masked.length());
        assertTrue(masked.contains(" "), "the space is revealed");
        assertTrue(masked.startsWith("_"), "letters are hidden");
        assertFalse(masked.contains("i"), "no letters revealed yet");
    }

    @Test
    void revealHintAlwaysKeepsOneLetterHidden() {
        Room room = roomWith(2, 2);
        Game game = new Game(room);
        game.start();
        game.setWordOptions(List.of("apple"));
        game.chooseWord("apple");

        for (int i = 0; i < 10; i++) {
            game.revealNextHint();
        }
        long hidden = game.maskedWord().chars().filter(c -> c == '_').count();
        assertTrue(hidden >= 1, "at least one letter stays hidden");
    }

    @Test
    void turnsRotateThroughEveryPlayerEachRoundThenEnd() {
        Room room = roomWith(2, 2); // 2 players x 2 rounds = 4 turns
        Game game = new Game(room);
        game.start();

        int turns = 1; // start() set up the first turn
        while (game.advance()) {
            turns++;
            assertEquals(GamePhase.CHOOSING, game.getPhase());
        }
        assertEquals(GamePhase.GAME_OVER, game.getPhase());
        assertEquals(4, turns);
    }

    @Test
    void earlierGuessScoresHigherThanFractionFloor() {
        Room room = roomWith(2, 2);
        Game game = new Game(room);
        game.start();
        game.setWordOptions(List.of("apple"));
        game.chooseWord("apple");
        String drawer = game.getCurrentDrawerId();
        String guesser = drawer.equals("p0") ? "p1" : "p0";

        game.registerGuess(guesser, "apple");
        // Guessing immediately (almost full time left) should be well above the 50-point floor.
        assertTrue(room.getPlayer(guesser).getScore() >= 50);
    }
}
