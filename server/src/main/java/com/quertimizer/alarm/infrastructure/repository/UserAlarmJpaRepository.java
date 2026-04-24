package com.quertimizer.alarm.infrastructure.repository;

import com.quertimizer.alarm.application.port.UserAlarmRepository;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAlarmJpaRepository extends JpaRepository<UserAlarm, Long>, UserAlarmRepository {
}
