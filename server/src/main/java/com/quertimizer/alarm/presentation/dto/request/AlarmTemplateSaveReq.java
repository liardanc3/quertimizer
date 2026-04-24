package com.quertimizer.alarm.presentation.dto.request;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
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

    public AlarmTemplateInput toAlarmTemplateInput() {
        return new AlarmTemplateInput(sentence, description);
    }
}
