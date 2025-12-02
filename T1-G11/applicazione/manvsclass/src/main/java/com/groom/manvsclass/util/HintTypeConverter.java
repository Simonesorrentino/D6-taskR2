package com.groom.manvsclass.util;

import com.groom.manvsclass.model.enums.HintTypeEnum;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class HintTypeConverter implements AttributeConverter<HintTypeEnum, String> {

    /**
     * Converte l'Enum in Stringa per il salvataggio nel database.
     * @param attribute l'oggetto Enum Java
     * @return la stringa in minuscolo da salvare nel DB
     */
    @Override
    public String convertToDatabaseColumn(HintTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getValue();
    }

    /**
     * Converte la stringa del database in oggetto Enum.
     * @param dbData la stringa letta dal DB
     * @return l'oggetto Enum corrispondente
     */
    @Override
    public HintTypeEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return HintTypeEnum.fromValue(dbData);
    }
}
