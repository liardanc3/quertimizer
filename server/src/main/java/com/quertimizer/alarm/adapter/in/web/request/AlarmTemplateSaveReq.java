package com.quertimizer.alarm.adapter.in.web.request;

import com.quertimizer.alarm.application.input.AlarmTemplateInput;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AlarmTemplateSaveReq {

    @NotBlank
    private String sentence;

    @NotBlank
    private String description;

    public AlarmTemplateInput toAlarmTemplateInput(String alarmType) {
        return new AlarmTemplateInput(alarmType, sentence, description);
    }
}
