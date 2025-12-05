package com.groom.manvsclass.model.dto;

import com.groom.manvsclass.model.enums.HintTypeEnum;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hint {

    @NotNull
    private String classUTName;

    @NotNull
    private String content;

    private String imageUri;

    @NotNull
    private HintTypeEnum type;

    @NotNull
    private Integer order;

}
