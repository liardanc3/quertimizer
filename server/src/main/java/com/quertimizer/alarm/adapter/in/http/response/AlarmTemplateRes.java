package com.quertimizer.alarm.adapter.in.http.response;

import com.quertimizer.alarm.application.output.AlarmTemplateOutput;
import lombok.Data;

@Data
public class AlarmTemplateRes {

    private final String type;
    private final String sentence;
    private final String description;

    public static AlarmTemplateRes from(AlarmTemplateOutput alarmTemplate) {
        return new AlarmTemplateRes(
                alarmTemplate.getType(), alarmTemplate.getSentence(), alarmTemplate.getDescription()
        );
    }

}
