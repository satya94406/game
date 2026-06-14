package com.skribbl.game;

import com.skribbl.dto.Avatar;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomTest {

    private static final Avatar AVATAR = Avatar.defaultAvatar();

    @Test
    void firstPlayerBecomesHost() {
        Room room = new Room("R", new RoomSettings());
        room.addPlayer("a", "Alice", AVATAR);
        room.addPlayer("b", "Bob", AVATAR);
        assertTrue(room.isHost("a"));
        assertFalse(room.isHost("b"));
        assertEquals(2, room.size());
    }

    @Test
    void removingHostPromotesTheNextPlayer() {
        Room room = new Room("R", new RoomSettings());
        room.addPlayer("a", "Alice", AVATAR);
        room.addPlayer("b", "Bob", AVATAR);
        room.removePlayer("a");
        assertEquals("b", room.getHostId());
        assertTrue(room.isHost("b"));
    }

    @Test
    void isFullRespectsMaxPlayers() {
        RoomSettings settings = new RoomSettings();
        settings.setMaxPlayers(2);
        Room room = new Room("R", settings);
        room.addPlayer("a", "A", AVATAR);
        assertFalse(room.isFull());
        room.addPlayer("b", "B", AVATAR);
        assertTrue(room.isFull());
    }

    @Test
    void disconnectKeepsPlayerButDropsFromConnectedSet() {
        Room room = new Room("R", new RoomSettings());
        room.addPlayer("a", "A", AVATAR);
        room.addPlayer("b", "B", AVATAR);
        room.markDisconnected("a");
        assertFalse(room.isConnected("a"));
        assertEquals(1, room.connectedCount());
        assertEquals(List.of("b"), room.connectedPlayerIds());
    }

    @Test
    void rejoinWithSameIdReconnects() {
        Room room = new Room("R", new RoomSettings());
        room.addPlayer("a", "Alice", AVATAR);
        room.markDisconnected("a");
        assertFalse(room.isConnected("a"));
        room.addPlayer("a", "Alice", AVATAR); // same id rejoins
        assertTrue(room.isConnected("a"));
        assertEquals(1, room.size());
    }
}
