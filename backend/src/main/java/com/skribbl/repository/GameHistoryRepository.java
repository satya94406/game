package com.skribbl.repository;

import com.skribbl.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    List<GameHistory> findTop20ByOrderByFinishedAtDesc();
}
