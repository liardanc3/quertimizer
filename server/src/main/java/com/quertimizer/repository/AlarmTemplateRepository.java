package com.quertimizer.repository;

import com.quertimizer.entity.AlarmTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmTemplateRepository extends JpaRepository<AlarmTemplate, String> {

    List<AlarmTemplate> findAllByOrderByAlarmTypeAsc();

}
