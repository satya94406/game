package com.skribbl.controller;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;

final class Sessions {

    private Sessions() {
    }

    static String playerId(SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        return attrs == null ? null : (String) attrs.get("playerId");
    }
}
