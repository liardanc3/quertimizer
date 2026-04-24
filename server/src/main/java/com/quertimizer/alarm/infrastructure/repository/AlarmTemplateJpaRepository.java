package com.quertimizer.alarm.infrastructure.repository;

import com.quertimizer.alarm.application.port.AlarmTemplateRepository;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmTemplateJpaRepository extends JpaRepository<AlarmTemplate, String>, AlarmTemplateRepository {
}
