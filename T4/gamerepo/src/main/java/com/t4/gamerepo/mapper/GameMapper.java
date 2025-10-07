package com.t4.gamerepo.mapper;

import com.t4.gamerepo.model.Game;
import com.t4.gamerepo.model.dto.response.GameDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GameMapper {
    GameMapper INSTANCE = Mappers.getMapper(GameMapper.class);

    GameDTO gameToGameDTO(Game game);
}
