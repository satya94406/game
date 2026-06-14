package com.skribbl.controller;

import com.skribbl.dto.In;
import com.skribbl.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/** Room/lobby/turn STOMP actions: {@code /app/room/{code}/...}. */
@Controller
public class RoomSocketController {

    private final GameService gameService;

    public RoomSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/room/{code}/join")
    public void join(@DestinationVariable String code, @Payload In.Join msg, SimpMessageHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            attrs.put("playerId", msg.playerId());
            attrs.put("code", code);
        }
        gameService.handleJoin(code, msg);
    }

    @MessageMapping("/room/{code}/leave")
    public void leave(@DestinationVariable String code, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleLeave(code, playerId);
        }
    }

    @MessageMapping("/room/{code}/start")
    public void start(@DestinationVariable String code, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleStart(code, playerId);
        }
    }

    @MessageMapping("/room/{code}/word")
    public void word(@DestinationVariable String code, @Payload In.WordChosen msg, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleWordChosen(code, playerId, msg.word());
        }
    }

    @MessageMapping("/room/{code}/settings")
    public void settings(@DestinationVariable String code, @Payload In.UpdateSettings msg, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleUpdateSettings(code, playerId, msg.settings());
        }
    }

    @MessageMapping("/room/{code}/playAgain")
    public void playAgain(@DestinationVariable String code, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handlePlayAgain(code, playerId);
        }
    }
}
