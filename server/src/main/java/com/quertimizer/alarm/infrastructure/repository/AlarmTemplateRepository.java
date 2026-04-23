package com.quertimizer.alarm.infrastructure.repository;

import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmTemplateRepository extends JpaRepository<AlarmTemplate, String> {

    List<AlarmTemplate> findAllByOrderByAlarmTypeAsc();

}
