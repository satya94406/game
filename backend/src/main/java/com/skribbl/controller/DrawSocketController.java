package com.skribbl.controller;

import com.skribbl.dto.In;
import com.skribbl.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/** Drawing STOMP actions (drawer-only, enforced server-side): draw / clear / undo. */
@Controller
public class DrawSocketController {

    private final GameService gameService;

    public DrawSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/room/{code}/draw")
    public void draw(@DestinationVariable String code, @Payload In.Draw msg, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleDraw(code, playerId, msg);
        }
    }

    @MessageMapping("/room/{code}/clear")
    public void clear(@DestinationVariable String code, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleClear(code, playerId);
        }
    }

    @MessageMapping("/room/{code}/undo")
    public void undo(@DestinationVariable String code, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleUndo(code, playerId);
        }
    }
}
