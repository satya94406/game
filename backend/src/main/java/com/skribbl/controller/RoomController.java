package com.skribbl.controller;

import com.skribbl.dto.In;
import com.skribbl.game.GamePhase;
import com.skribbl.game.Room;
import com.skribbl.game.RoomManager;
import com.skribbl.game.RoomSettings;
import com.skribbl.service.GameHistoryService;
import com.skribbl.service.WordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomManager roomManager;
    private final WordService wordService;
    private final GameHistoryService historyService;

    public RoomController(RoomManager roomManager, WordService wordService, GameHistoryService historyService) {
        this.roomManager = roomManager;
        this.wordService = wordService;
        this.historyService = historyService;
    }

    @PostMapping("/rooms")
    public Map<String, String> create(@RequestBody(required = false) In.CreateRoom request) {
        RoomSettings settings = (request != null && request.settings() != null)
                ? request.settings()
                : new RoomSettings();
        Room room = roomManager.createRoom(settings);
        return Map.of("code", room.getCode());
    }

    @GetMapping("/rooms")
    public List<Map<String, Object>> publicRooms() {
        return roomManager.publicRooms().stream().map(RoomController::summary).toList();
    }

    @GetMapping("/rooms/{code}")
    public ResponseEntity<Map<String, Object>> peek(@PathVariable String code) {
        Room room = roomManager.getRoom(code);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = summary(room);
        body.put("phase", room.getPhase().name());
        body.put("started", room.getPhase() != GamePhase.LOBBY);
        body.put("full", room.isFull());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/categories")
    public List<String> categories(@RequestParam(defaultValue = "en") String lang) {
        return wordService.categories(lang);
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return historyService.recent().stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roomCode", h.getRoomCode());
            m.put("rounds", h.getRounds());
            m.put("players", h.getPlayerCount());
            m.put("winnerName", h.getWinnerName());
            m.put("winnerScore", h.getWinnerScore());
            m.put("finishedAt", h.getFinishedAt());
            return m;
        }).toList();
    }

    private static Map<String, Object> summary(Room room) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", room.getCode());
        m.put("players", room.connectedCount());
        m.put("maxPlayers", room.getSettings().getMaxPlayers());
        m.put("rounds", room.getSettings().getRounds());
        return m;
    }
}
