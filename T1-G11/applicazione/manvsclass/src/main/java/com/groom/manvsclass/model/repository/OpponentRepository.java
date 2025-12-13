package com.groom.manvsclass.model.repository;

import com.groom.manvsclass.model.Opponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import testrobotchallenge.commons.models.opponent.OpponentDifficulty;
import testrobotchallenge.commons.models.score.EvosuiteScore;
import testrobotchallenge.commons.models.score.JacocoScore;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpponentRepository extends JpaRepository<Opponent, Long> {

    Optional<Opponent> findByClassUT_NameAndOpponentTypeAndDifficulty(
            String classUTName,
            String opponentType,
            OpponentDifficulty difficulty
    );
}
