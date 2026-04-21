package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.AlarmTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlarmTemplateRes {

    private final String type;
    private final String sentence;
    private final String description;

    public static AlarmTemplateRes from(AlarmTemplate alarmTemplate) {
        return new AlarmTemplateRes(
                alarmTemplate.getAlarmType(),
                alarmTemplate.getSentence(),
                alarmTemplate.getDescription()
        );
    }

}
