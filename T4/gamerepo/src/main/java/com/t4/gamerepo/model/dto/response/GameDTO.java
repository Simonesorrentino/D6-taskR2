package com.t4.gamerepo.model.dto.response;

import com.t4.gamerepo.model.GameStatus;
import com.t4.gamerepo.model.PlayerResult;
import com.t4.gamerepo.model.Round;
import testrobotchallenge.commons.models.opponent.GameMode;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public record GameDTO(
    Long id,
    List<Long> players,
    GameStatus status,
    GameMode gameMode,
    Map<Long, PlayerResult> playerResults,
    List<Round> rounds,
    Timestamp startedAt,
    Timestamp closedAt
) {
}
