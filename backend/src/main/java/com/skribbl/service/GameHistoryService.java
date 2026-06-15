package com.skribbl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skribbl.dto.PlayerDto;
import com.skribbl.entity.GameHistory;
import com.skribbl.game.Game;
import com.skribbl.game.Room;
import com.skribbl.repository.GameHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameHistoryService {

    private static final Logger log = LoggerFactory.getLogger(GameHistoryService.class);

    private final GameHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public GameHistoryService(GameHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(Room room, List<PlayerDto> leaderboard, Game game) {
        try {
            GameHistory history = new GameHistory();
            history.setRoomCode(room.getCode());
            history.setRounds(game != null ? game.getTotalRounds() : 0);
            history.setPlayerCount(leaderboard.size());
            if (!leaderboard.isEmpty()) {
                history.setWinnerName(leaderboard.get(0).name());
                history.setWinnerScore(leaderboard.get(0).score());
            }
            List<Map<String, Object>> rows = leaderboard.stream()
                    .map(p -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("name", p.name());
                        row.put("score", p.score());
                        return row;
                    })
                    .toList();
            history.setLeaderboardJson(objectMapper.writeValueAsString(rows));
            repository.save(history);
        } catch (Exception e) {
            log.warn("Failed to persist game history for room {}", room.getCode(), e);
        }
    }

    public List<GameHistory> recent() {
        return repository.findTop20ByOrderByFinishedAtDesc();
    }
}
