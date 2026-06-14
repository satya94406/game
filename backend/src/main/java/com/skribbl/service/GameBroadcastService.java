package com.skribbl.service;

import com.skribbl.dto.ServerMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over {@link SimpMessagingTemplate} that centralizes the room
 * destination conventions:
 * <ul>
 *   <li>{@code /topic/room/{code}} — broadcast to everyone in the room</li>
 *   <li>{@code /topic/room/{code}/u/{playerId}} — a single player (e.g. drawer word options)</li>
 * </ul>
 */
@Service
public class GameBroadcastService {

    private final SimpMessagingTemplate template;

    public GameBroadcastService(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void toRoom(String code, String type, Object data) {
        template.convertAndSend(roomDest(code), ServerMessage.of(type, data));
    }

    /** Send a pre-built envelope (used for draw events that are also stored for replay). */
    public void toRoom(String code, ServerMessage message) {
        template.convertAndSend(roomDest(code), message);
    }

    public void toPlayer(String code, String playerId, String type, Object data) {
        template.convertAndSend(roomDest(code) + "/u/" + playerId, ServerMessage.of(type, data));
    }

    private static String roomDest(String code) {
        return "/topic/room/" + code;
    }
}
