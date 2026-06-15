package com.skribbl.game;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class RoomManager {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoom = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Room createRoom(RoomSettings settings) {
        settings.clamp();
        String code = uniqueCode();
        Room room = new Room(code, settings);
        rooms.put(code, room);
        return room;
    }

    public Room getRoom(String code) {
        return code == null ? null : rooms.get(code.toUpperCase());
    }

    public void removeRoom(String code) {
        if (code != null) {
            rooms.remove(code.toUpperCase());
        }
    }

    public void registerPlayer(String playerId, String code) {
        playerRoom.put(playerId, code);
    }

    public String roomOfPlayer(String playerId) {
        return playerRoom.get(playerId);
    }

    public void unregisterPlayer(String playerId) {
        playerRoom.remove(playerId);
    }

    public List<Room> publicRooms() {
        List<Room> list = new ArrayList<>();
        for (Room r : rooms.values()) {
            if (r.getSettings().isPublicRoom()
                    && r.getPhase() == GamePhase.LOBBY
                    && !r.isFull()
                    && r.connectedCount() > 0) {
                list.add(r);
            }
        }
        return list;
    }

    public int roomCount() {
        return rooms.size();
    }

    private String uniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }
}
