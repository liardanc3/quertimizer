package com.quertimizer.alarm.adapter.in.http.response;

import com.quertimizer.alarm.application.output.AlarmItemOutput;
import com.quertimizer.alarm.domain.model.AlarmBinding;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AlarmItemRes {

    private final Long alarmId;
    private final String alarmType;
    private final String title;
    private final String message;
    private final String sentence;
    private final String description;
    private final Map<String, AlarmBinding> bindings;
    private final String targetPath;
    private final String targetHash;
    private final boolean read;
    private final LocalDateTime createdAt;

    public static AlarmItemRes from(AlarmItemOutput result) {
        return new AlarmItemRes(
                result.getAlarmId(),
                result.getAlarmType(),
                result.getTitle(),
                result.getMessage(),
                result.getSentence(),
                result.getDescription(),
                result.getBindings(),
                result.getTargetPath(),
                result.getTargetHash(),
                result.isRead(),
                result.getCreatedAt()
        );
    }
}
