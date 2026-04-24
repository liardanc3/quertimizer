package com.quertimizer.alarm.application.port;

import com.quertimizer.alarm.domain.entity.AlarmTemplate;

import java.util.List;
import java.util.Optional;

public interface AlarmTemplateRepository {

    List<AlarmTemplate> findAllByOrderByAlarmTypeAsc();

    Optional<AlarmTemplate> findById(String alarmType);

    <S extends AlarmTemplate> S save(S alarmTemplate);
}
