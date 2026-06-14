package com.skribbl.controller;

import com.skribbl.dto.In;
import com.skribbl.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/** Guess + chat STOMP actions. The in-game text box sends guesses; the server classifies them. */
@Controller
public class ChatSocketController {

    private final GameService gameService;

    public ChatSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/room/{code}/guess")
    public void guess(@DestinationVariable String code, @Payload In.Guess msg, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleGuess(code, playerId, msg.text());
        }
    }

    @MessageMapping("/room/{code}/chat")
    public void chat(@DestinationVariable String code, @Payload In.Chat msg, SimpMessageHeaderAccessor accessor) {
        String playerId = Sessions.playerId(accessor);
        if (playerId != null) {
            gameService.handleChat(code, playerId, msg.text());
        }
    }
}
