package com.quertimizer.alarm.application.port.out;

import com.quertimizer.alarm.domain.entity.AlarmTemplate;

import java.util.List;
import java.util.Optional;

public interface AlarmTemplateRepositoryPort {

    List<AlarmTemplate> findAllByOrderByAlarmTypeAsc();

    Optional<AlarmTemplate> findById(String alarmType);

    AlarmTemplate save(AlarmTemplate alarmTemplate);
}
