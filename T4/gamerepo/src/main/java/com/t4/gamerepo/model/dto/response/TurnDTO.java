package com.t4.gamerepo.model.dto.response;

import com.t4.gamerepo.model.TurnScore;

import java.sql.Timestamp;

public record TurnDTO (
    Long id,
    Long playerId,
    int turnNumber,
    TurnScore score,
    Timestamp startedAt,
    Timestamp closedAt
) {
}
