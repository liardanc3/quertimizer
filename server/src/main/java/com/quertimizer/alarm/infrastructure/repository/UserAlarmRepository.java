package com.quertimizer.alarm.infrastructure.repository;

import com.quertimizer.alarm.domain.entity.UserAlarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    Page<UserAlarm> findAllByHandleOrderByCreatedAtDescAlarmIdDesc(String handle, Pageable pageable);

    Optional<UserAlarm> findByAlarmIdAndHandle(Long alarmId, String handle);

    List<UserAlarm> findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(String handle);

    long countByHandleAndReadFalse(String handle);

}
