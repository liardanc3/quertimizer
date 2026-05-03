package com.quertimizer.alarm.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmTemplateJpaRepository extends JpaRepository<AlarmTemplateJpaEntity, String> {
    List<AlarmTemplateJpaEntity> findAllByOrderByAlarmTypeAsc();
}
