package com.skribbl.listener;

import com.skribbl.service.GameService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Detects dropped connections. The {@code playerId} and room {@code code} stored in
 * the session attributes on join are read back here so the player can be removed and
 * the game advanced (drawer-left, host promotion, empty-room cleanup).
 */
@Component
public class WebSocketEventListener {

    private final GameService gameService;

    public WebSocketEventListener(GameService gameService) {
        this.gameService = gameService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Map<String, Object> attrs = SimpMessageHeaderAccessor.wrap(event.getMessage()).getSessionAttributes();
        if (attrs == null) {
            return;
        }
        String playerId = (String) attrs.get("playerId");
        String code = (String) attrs.get("code");
        if (playerId != null && code != null) {
            gameService.handleDisconnect(code, playerId);
        }
    }
}
