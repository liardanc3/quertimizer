package com.quertimizer.alarm.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmTemplateSaveReq {

    @NotBlank
    private String sentence;

    @NotBlank
    private String description;

}
