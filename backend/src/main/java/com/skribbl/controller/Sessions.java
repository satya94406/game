package com.skribbl.controller;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

/**
 * Reads the {@code playerId} that {@code join} stored in the STOMP session
 * attributes. Every action other than join is attributed to the session's
 * player, so a client can never act on another player's behalf.
 */
final class Sessions {

    private Sessions() {
    }

    static String playerId(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        return attrs == null ? null : (String) attrs.get("playerId");
    }
}
