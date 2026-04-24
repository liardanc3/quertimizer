package com.quertimizer.alarm.presentation.dto.response;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmTemplateRes {

    private final String type;
    private final String sentence;
    private final String description;

    public static AlarmTemplateRes from(AlarmTemplateOutput alarmTemplate) {
        return new AlarmTemplateRes(
                alarmTemplate.getType(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription()
        );
    }

}
