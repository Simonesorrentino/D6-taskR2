package com.t4.gamerepo.mapper;

import com.t4.gamerepo.model.Round;
import com.t4.gamerepo.model.dto.response.RoundDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RoundMapper {
    RoundMapper INSTANCE = Mappers.getMapper(RoundMapper.class);

    RoundDTO toRoundDTO(Round round);
}
