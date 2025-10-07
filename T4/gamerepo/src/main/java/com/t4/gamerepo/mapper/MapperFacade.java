package com.t4.gamerepo.mapper;

import com.t4.gamerepo.model.*;
import com.t4.gamerepo.model.dto.request.CloseGameDTO;
import com.t4.gamerepo.model.dto.request.CloseTurnDTO;
import com.t4.gamerepo.model.dto.response.GameDTO;
import com.t4.gamerepo.model.dto.response.RoundDTO;
import com.t4.gamerepo.model.dto.response.TurnDTO;

public class MapperFacade {
    private static final GameMapper gameMapper = GameMapper.INSTANCE;
    private static final RoundMapper roundMapper = RoundMapper.INSTANCE;
    private static final TurnMapper turnMapper = TurnMapper.INSTANCE;

    private MapperFacade() {
        throw new IllegalStateException("Classe che raccoglie i mapper definiti nel modulo");
    }

    public static GameDTO toDTO(Game game) {
        return gameMapper.gameToGameDTO(game);
    }

    public static RoundDTO toDTO(Round round) {
        return roundMapper.toRoundDTO(round);
    }

    public static TurnDTO toDTO(Turn turn) {
        return turnMapper.toTurnDTO(turn);
    }

    public static TurnScore toEntity(CloseTurnDTO dto) {
        return TurnScoreMapper.toTurnScore(dto);
    }

    public static PlayerResult toEntity(CloseGameDTO.PlayerResultDTO dto) {
        return PlayerResultMapper.toPlayerResult(dto);
    }


}
