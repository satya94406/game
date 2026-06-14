package com.skribbl.game;

import com.skribbl.dto.Avatar;
import com.skribbl.dto.ServerMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A room: the durable container for players, settings, the canvas draw log, and
 * (once started) a {@link Game}. All mutating methods synchronize on the room
 * instance; {@code GameService} synchronizes on the same monitor for compound
 * operations so guesses and turn transitions never race.
 */
public class Room {

    private final String code;
    private final RoomSettings settings;
    private final LinkedHashMap<String, Player> players = new LinkedHashMap<>(); // join order
    private String hostId;
    private Game game;

    /** Current turn's draw events, replayed to late joiners; reset each turn and on clear. */
    private final List<ServerMessage> drawLog = new ArrayList<>();

    public Room(String code, RoomSettings settings) {
        this.code = code;
        this.settings = settings;
    }

    // --- players ---

    public synchronized Player addPlayer(String id, String name, Avatar avatar) {
        Player existing = players.get(id);
        if (existing != null) { // reconnect / refresh of an existing identity
            existing.setConnected(true);
            existing.setName(name);
            if (avatar != null) {
                existing.setAvatar(avatar);
            }
            return existing;
        }
        Player p = new Player(id, name, avatar);
        if (players.isEmpty()) {
            p.setHost(true);
            hostId = id;
        }
        players.put(id, p);
        return p;
    }

    public synchronized Player removePlayer(String id) {
        Player removed = players.remove(id);
        if (removed != null && id.equals(hostId)) {
            Iterator<Player> it = players.values().iterator();
            if (it.hasNext()) {
                Player next = it.next();
                next.setHost(true);
                hostId = next.getId();
            } else {
                hostId = null;
            }
        }
        return removed;
    }

    public synchronized void markDisconnected(String id) {
        Player p = players.get(id);
        if (p != null) {
            p.setConnected(false);
        }
    }

    public synchronized Player getPlayer(String id) {
        return players.get(id);
    }

    public synchronized boolean isConnected(String id) {
        Player p = players.get(id);
        return p != null && p.isConnected();
    }

    public synchronized boolean isHost(String id) {
        return id != null && id.equals(hostId);
    }

    public synchronized String getHostId() {
        return hostId;
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players.values());
    }

    public synchronized List<Player> getConnectedPlayers() {
        List<Player> list = new ArrayList<>();
        for (Player p : players.values()) {
            if (p.isConnected()) {
                list.add(p);
            }
        }
        return list;
    }

    public synchronized List<String> connectedPlayerIds() {
        List<String> ids = new ArrayList<>();
        for (Player p : players.values()) {
            if (p.isConnected()) {
                ids.add(p.getId());
            }
        }
        return ids;
    }

    public synchronized int connectedCount() {
        int n = 0;
        for (Player p : players.values()) {
            if (p.isConnected()) {
                n++;
            }
        }
        return n;
    }

    public synchronized int size() {
        return players.size();
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    public synchronized boolean isFull() {
        return players.size() >= settings.getMaxPlayers();
    }

    // --- canvas draw log (replayed to late joiners) ---

    public synchronized void recordDrawEvent(ServerMessage msg) {
        drawLog.add(msg);
    }

    public synchronized void clearCanvas() {
        drawLog.clear();
    }

    public synchronized List<ServerMessage> getDrawLog() {
        return new ArrayList<>(drawLog);
    }

    // --- accessors ---

    public String getCode() {
        return code;
    }

    public RoomSettings getSettings() {
        return settings;
    }

    public GamePhase getPhase() {
        return game == null ? GamePhase.LOBBY : game.getPhase();
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}
