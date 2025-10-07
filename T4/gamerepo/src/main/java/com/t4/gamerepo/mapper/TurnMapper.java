package com.t4.gamerepo.mapper;

import com.t4.gamerepo.model.Turn;
import com.t4.gamerepo.model.dto.response.TurnDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TurnMapper {
    TurnMapper INSTANCE = Mappers.getMapper(TurnMapper.class);

    TurnDTO toTurnDTO(Turn turn);
}
