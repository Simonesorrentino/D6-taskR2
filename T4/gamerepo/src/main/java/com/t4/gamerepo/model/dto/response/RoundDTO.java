package com.t4.gamerepo.model.dto.response;

import com.t4.gamerepo.model.Turn;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.opponent.OpponentType;

import java.sql.Timestamp;
import java.util.List;

public record RoundDTO(
    Long id,
    String classUT,
    OpponentType type,
    OpponentDifficulty difficulty,
    int roundNumber,
    List<Turn> turns,
    Timestamp startedAt,
    Timestamp closedAt
) {
}
