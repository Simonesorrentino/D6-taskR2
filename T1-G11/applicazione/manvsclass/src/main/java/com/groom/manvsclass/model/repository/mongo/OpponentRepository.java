package com.groom.manvsclass.model.repository.mongo;

import com.groom.manvsclass.model.OpponentMongoDB;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.score.EvosuiteScore;
import testrobotchallenge.commons.models.score.JacocoScore;

import java.util.List;
import java.util.Optional;

public interface OpponentRepository {
    void saveOpponent(OpponentMongoDB opponentMongoDB);

    List<OpponentMongoDB> findAllOpponents();

    Optional<OpponentMongoDB> findOpponent(String classUT, String type, OpponentDifficulty difficulty);

    Optional<EvosuiteScore> findEvosuiteScore(String classUT, String type, OpponentDifficulty difficulty);

    Optional<JacocoScore> findJacocoScore(String classUT, String type, OpponentDifficulty difficulty);

    Optional<String> findCoverage(String classUT, String type, OpponentDifficulty difficulty);
}
