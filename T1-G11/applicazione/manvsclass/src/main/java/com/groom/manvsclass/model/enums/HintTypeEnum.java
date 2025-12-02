package com.groom.manvsclass.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum HintTypeEnum {

    GENERIC("generic"),
    CLASS_UT("class");

    private final String value;

    HintTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static HintTypeEnum fromValue(String text) {
        for (HintTypeEnum b : HintTypeEnum.values()) {
            if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}