package com.skribbl.service;

import com.skribbl.dto.In;
import com.skribbl.dto.Out;
import com.skribbl.dto.PlayerDto;
import com.skribbl.dto.ServerMessage;
import com.skribbl.game.Game;
import com.skribbl.game.GamePhase;
import com.skribbl.game.Player;
import com.skribbl.game.Room;
import com.skribbl.game.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * The orchestration hub ("message handler") that turns inbound client actions and
 * fired timers into mutations of the in-memory {@link Room}/{@link Game} model plus
 * outbound broadcasts. Controllers and the disconnect listener stay thin and delegate here.
 *
 * <p>Phase flow per turn: CHOOSING (drawer picks a word, with an auto-pick timeout) →
 * DRAWING (round timer + spaced hint reveals) → ROUND_END (scores shown) → next turn,
 * until all rounds are done → GAME_OVER (leaderboard persisted to MySQL).
 */
@Service
public class GameService {

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    private static final int CHOOSE_SECONDS = 15;
    private static final int ROUND_END_SECONDS = 6;

    private final RoomManager roomManager;
    private final WordService wordService;
    private final GameBroadcastService broadcast;
    private final GameTimerService timer;
    private final GameHistoryService historyService;

    public GameService(RoomManager roomManager,
                       WordService wordService,
                       GameBroadcastService broadcast,
                       GameTimerService timer,
                       GameHistoryService historyService) {
        this.roomManager = roomManager;
        this.wordService = wordService;
        this.broadcast = broadcast;
        this.timer = timer;
        this.historyService = historyService;
    }

    // ===================== inbound actions =====================

    public void handleJoin(String code, In.Join msg) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            broadcast.toPlayer(code, msg.playerId(), "ERROR", new Out.Error("Room not found"));
            return;
        }
        Player player;
        boolean full = false;
        synchronized (room) {
            if (room.getPlayer(msg.playerId()) == null && room.isFull()) {
                full = true;
                player = null;
            } else {
                player = room.addPlayer(msg.playerId(), msg.name(), msg.avatar());
                roomManager.registerPlayer(msg.playerId(), code);
            }
        }
        if (full) {
            broadcast.toPlayer(code, msg.playerId(), "ERROR", new Out.Error("Room is full"));
            return;
        }

        broadcast.toPlayer(code, player.getId(), "JOINED",
                new Out.Joined(player.getId(), code, room.getSettings(),
                        playerDtos(room), room.getHostId(), room.getPhase().name()));
        broadcastPlayers(room);
        broadcast.toRoom(code, "SYSTEM", new Out.System(player.getName() + " joined"));

        if (room.getPhase() != GamePhase.LOBBY) {
            // catch a late joiner up: full snapshot incl. the canvas so far
            broadcast.toPlayer(code, player.getId(), "STATE", buildState(room, true));
        }
    }

    public void handleStart(String code, String playerId) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            if (!room.isHost(playerId)) {
                broadcast.toPlayer(code, playerId, "ERROR", new Out.Error("Only the host can start the game"));
                return;
            }
            if (room.getPhase() != GamePhase.LOBBY) {
                return;
            }
            if (room.connectedCount() < 2) {
                broadcast.toPlayer(code, playerId, "ERROR", new Out.Error("Need at least 2 players to start"));
                return;
            }
            Game game = new Game(room);
            room.setGame(game);
            game.start();
        }
        startChoosingPhase(room);
    }

    public void handleWordChosen(String code, String playerId, String word) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            Game game = room.getGame();
            if (game == null || game.getPhase() != GamePhase.CHOOSING
                    || !playerId.equals(game.getCurrentDrawerId())) {
                return;
            }
            if (!game.chooseWord(word)) {
                return;
            }
        }
        beginDrawingPhase(room);
    }

    public void handleDraw(String code, String playerId, In.Draw draw) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Game game = room.getGame();
        if (game == null || game.getPhase() != GamePhase.DRAWING
                || !playerId.equals(game.getCurrentDrawerId())) {
            return;
        }
        ServerMessage event = ServerMessage.of("DRAW", draw);
        room.recordDrawEvent(event);   // buffered for late-join replay
        broadcast.toRoom(code, event); // non-drawers render this; the drawer ignores its own echo
    }

    public void handleClear(String code, String playerId) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Game game = room.getGame();
        if (game == null || game.getPhase() != GamePhase.DRAWING
                || !playerId.equals(game.getCurrentDrawerId())) {
            return;
        }
        room.clearCanvas();
        broadcast.toRoom(code, "CLEAR", null);
    }

    public void handleUndo(String code, String playerId) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Game game = room.getGame();
        if (game == null || game.getPhase() != GamePhase.DRAWING
                || !playerId.equals(game.getCurrentDrawerId())) {
            return;
        }
        ServerMessage event = ServerMessage.of("UNDO", null);
        room.recordDrawEvent(event);
        broadcast.toRoom(code, event);
    }

    public void handleGuess(String code, String playerId, String text) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Player player = room.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Game game = room.getGame();
        if (game == null || game.getPhase() != GamePhase.DRAWING) {
            broadcast.toRoom(code, "CHAT", new Out.Chat(playerId, player.getName(), clean(text)));
            return;
        }

        Game.GuessOutcome outcome;
        boolean everyoneGuessed = false;
        synchronized (room) {
            outcome = game.registerGuess(playerId, text);
            if (outcome == Game.GuessOutcome.CORRECT) {
                everyoneGuessed = game.everyoneGuessed();
            }
        }

        switch (outcome) {
            case CORRECT -> {
                broadcast.toRoom(code, "GUESS", new Out.Guess(playerId, player.getName(), true, null));
                broadcastPlayers(room); // live scores + "guessed" badges
                if (everyoneGuessed) {
                    endTurn(code, "all-guessed");
                }
            }
            case CLOSE -> {
                broadcast.toRoom(code, "GUESS", new Out.Guess(playerId, player.getName(), false, clean(text)));
                broadcast.toPlayer(code, playerId, "SYSTEM", new Out.System("'" + clean(text) + "' is close!"));
            }
            case WRONG ->
                    broadcast.toRoom(code, "GUESS", new Out.Guess(playerId, player.getName(), false, clean(text)));
            case IGNORED -> {
                // Drawer or an already-correct player typing: relay as chat, but never leak the word.
                if (!Game.normalize(text).equals(Game.normalize(game.getCurrentWord()))) {
                    broadcast.toRoom(code, "CHAT", new Out.Chat(playerId, player.getName(), clean(text)));
                }
            }
            default -> {
            }
        }
    }

    public void handleChat(String code, String playerId, String text) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Player player = room.getPlayer(playerId);
        if (player == null) {
            return;
        }
        broadcast.toRoom(code, "CHAT", new Out.Chat(playerId, player.getName(), clean(text)));
    }

    public void handleUpdateSettings(String code, String playerId, com.skribbl.game.RoomSettings incoming) {
        Room room = roomManager.getRoom(code);
        if (room == null || incoming == null) {
            return;
        }
        synchronized (room) {
            if (!room.isHost(playerId) || room.getPhase() != GamePhase.LOBBY) {
                return;
            }
            room.getSettings().copyFrom(incoming);
        }
        broadcast.toRoom(code, "STATE", buildState(room, false));
    }

    public void handlePlayAgain(String code, String playerId) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            if (!room.isHost(playerId) || room.getPhase() != GamePhase.GAME_OVER) {
                return;
            }
            room.setGame(null);
            for (Player p : room.getPlayers()) {
                p.setScore(0);
                p.setDrawing(false);
            }
            room.clearCanvas();
        }
        timer.cancelAll(code);
        broadcast.toRoom(code, "SYSTEM", new Out.System("Back to the lobby — ready up!"));
        broadcast.toRoom(code, "STATE", buildState(room, false));
        broadcastPlayers(room);
    }

    public void handleLeave(String code, String playerId) {
        removePlayer(code, playerId);
    }

    public void handleDisconnect(String code, String playerId) {
        removePlayer(code, playerId);
    }

    // ===================== phase transitions =====================

    private void startChoosingPhase(Room room) {
        String code = room.getCode();
        timer.cancelAll(code);
        room.clearCanvas();

        Game game = room.getGame();
        String drawerId = game.getCurrentDrawerId();
        Player drawer = room.getPlayer(drawerId);
        if (drawer == null) { // drawer vanished between scheduling and firing
            nextTurn(code);
            return;
        }
        for (Player p : room.getPlayers()) {
            p.setDrawing(p.getId().equals(drawerId));
        }

        List<String> options = wordService.randomWords(room.getSettings());
        game.setWordOptions(options);

        broadcast.toRoom(code, "ROUND_START", new Out.RoundStart(
                game.getCurrentRound(), game.getTotalRounds(), drawerId, drawer.getName(), CHOOSE_SECONDS));
        broadcast.toPlayer(code, drawerId, "WORD_OPTIONS", new Out.WordOptions(options, CHOOSE_SECONDS));
        broadcast.toRoom(code, "STATE", buildState(room, false));
        broadcastPlayers(room);

        timer.schedule(code, CHOOSE_SECONDS, () -> autoChooseWord(code));
    }

    private void autoChooseWord(String code) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        synchronized (room) {
            Game game = room.getGame();
            if (game == null || game.getPhase() != GamePhase.CHOOSING) {
                return;
            }
            game.chooseWord(null); // falls back to the first option
        }
        beginDrawingPhase(room);
    }

    private void beginDrawingPhase(Room room) {
        String code = room.getCode();
        timer.cancelAll(code);

        broadcast.toRoom(code, "STATE", buildState(room, false));

        int drawTime = room.getSettings().getDrawTime();
        int hints = room.getSettings().getHints();
        for (int i = 1; i <= hints; i++) {
            int delay = (int) Math.round(drawTime * (double) i / (hints + 1));
            if (delay > 0 && delay < drawTime) {
                timer.schedule(code, delay, () -> revealHint(code));
            }
        }
        timer.schedule(code, drawTime, () -> endTurn(code, "time"));
    }

    private void revealHint(String code) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        String masked;
        synchronized (room) {
            Game game = room.getGame();
            if (game == null || game.getPhase() != GamePhase.DRAWING) {
                return;
            }
            masked = game.revealNextHint();
        }
        broadcast.toRoom(code, "HINT", new Out.Hint(masked));
    }

    private void endTurn(String code, String reason) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        Game game;
        synchronized (room) {
            game = room.getGame();
            if (game == null || game.getPhase() != GamePhase.DRAWING) {
                return; // already ended (timer raced with everyone-guessed)
            }
            game.endTurn();
        }
        timer.cancelAll(code);

        broadcast.toRoom(code, "ROUND_END",
                new Out.RoundEnd(game.getCurrentWord(), scoreRows(room, game), null, reason));
        broadcastPlayers(room);
        broadcast.toRoom(code, "STATE", buildState(room, false));

        timer.schedule(code, ROUND_END_SECONDS, () -> nextTurn(code));
    }

    private void nextTurn(String code) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return;
        }
        timer.cancelAll(code);

        boolean hasNext;
        synchronized (room) {
            Game game = room.getGame();
            if (game == null) {
                return;
            }
            if (room.connectedCount() < 2) {
                game.markGameOver();
                hasNext = false;
            } else {
                hasNext = game.advance();
            }
        }
        if (hasNext) {
            startChoosingPhase(room);
        } else {
            endGame(room);
        }
    }

    private void endGame(Room room) {
        String code = room.getCode();
        timer.cancelAll(code);

        Game game = room.getGame();
        if (game != null) {
            game.markGameOver();
        }
        List<PlayerDto> leaderboard = playerDtos(room); // already sorted by score desc
        PlayerDto winner = leaderboard.isEmpty() ? null : leaderboard.get(0);

        broadcast.toRoom(code, "GAME_OVER", new Out.GameOver(
                leaderboard,
                winner != null ? winner.id() : null,
                winner != null ? winner.name() : null));
        broadcast.toRoom(code, "STATE", buildState(room, false));

        historyService.save(room, leaderboard, game);
    }

    private void removePlayer(String code, String playerId) {
        Room room = roomManager.getRoom(code);
        roomManager.unregisterPlayer(playerId);
        if (room == null) {
            return;
        }

        boolean wasDrawer;
        boolean nowEmpty;
        GamePhase phase;
        synchronized (room) {
            Game game = room.getGame();
            wasDrawer = game != null && playerId.equals(game.getCurrentDrawerId());
            room.removePlayer(playerId);
            nowEmpty = room.isEmpty();
            phase = room.getPhase();
        }

        if (nowEmpty) {
            timer.cancelAll(code);
            roomManager.removeRoom(code);
            return;
        }

        broadcast.toRoom(code, "SYSTEM", new Out.System("A player left"));
        broadcastPlayers(room);

        if (phase == GamePhase.CHOOSING || phase == GamePhase.DRAWING) {
            if (room.connectedCount() < 2) {
                endGame(room);
                return;
            }
            if (wasDrawer) {
                timer.cancelAll(code);
                if (phase == GamePhase.DRAWING) {
                    endTurn(code, "drawer-left");
                } else {
                    nextTurn(code); // drawer left while choosing — skip to the next drawer
                }
            } else if (phase == GamePhase.DRAWING) {
                boolean everyone;
                synchronized (room) {
                    Game game = room.getGame();
                    everyone = game != null && game.everyoneGuessed();
                }
                if (everyone) {
                    endTurn(code, "all-guessed");
                }
            }
        }
    }

    // ===================== view helpers =====================

    private void broadcastPlayers(Room room) {
        broadcast.toRoom(room.getCode(), "PLAYERS", new Out.Players(playerDtos(room), room.getHostId()));
    }

    private List<PlayerDto> playerDtos(Room room) {
        Game game = room.getGame();
        return room.getPlayers().stream()
                .map(p -> toDto(p, game))
                .sorted(Comparator.comparingInt(PlayerDto::score).reversed())
                .toList();
    }

    private PlayerDto toDto(Player p, Game game) {
        boolean drawing = game != null && p.getId().equals(game.getCurrentDrawerId());
        boolean guessed = game != null && game.getGuessedPlayers().contains(p.getId());
        return new PlayerDto(p.getId(), p.getName(), p.getAvatar(), p.getScore(),
                p.isHost(), drawing, guessed, p.isConnected());
    }

    private List<Out.ScoreRow> scoreRows(Room room, Game game) {
        return room.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getScore).reversed())
                .map(p -> new Out.ScoreRow(p.getId(), p.getName(), p.getScore(),
                        game.getRoundGains().getOrDefault(p.getId(), 0)))
                .toList();
    }

    private Out.State buildState(Room room, boolean includeReplay) {
        Game game = room.getGame();
        GamePhase phase = room.getPhase();

        String drawerId = game != null ? game.getCurrentDrawerId() : null;
        Player drawer = drawerId != null ? room.getPlayer(drawerId) : null;
        String drawerName = drawer != null ? drawer.getName() : null;

        boolean drawing = game != null && phase == GamePhase.DRAWING;
        String masked = drawing ? game.maskedWord() : "";
        int wordLength = game != null ? game.wordLength() : 0;
        int timeLeft = drawing ? game.secondsLeft() : 0;

        return new Out.State(
                phase.name(),
                game != null ? game.getCurrentRound() : 0,
                game != null ? game.getTotalRounds() : room.getSettings().getRounds(),
                drawerId,
                drawerName,
                masked,
                wordLength,
                timeLeft,
                room.getSettings().getDrawTime(),
                playerDtos(room),
                room.getHostId(),
                room.getSettings(),
                includeReplay ? room.getDrawLog() : null);
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 240 ? trimmed.substring(0, 240) : trimmed;
    }
}
