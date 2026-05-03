package com.quertimizer.alarm.application.port.out;

import com.quertimizer.alarm.domain.entity.UserAlarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserAlarmRepositoryPort {

    Page<UserAlarm> findAllByHandleOrderByCreatedAtDescAlarmIdDesc(String handle, Pageable pageable);

    Page<UserAlarm> findAllByHandle(String handle, Pageable pageable);

    Optional<UserAlarm> findByAlarmIdAndHandle(Long alarmId, String handle);

    List<UserAlarm> findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(String handle);

    long countByHandleAndReadFalse(String handle);

    UserAlarm save(UserAlarm userAlarm);

    List<UserAlarm> saveAll(Iterable<UserAlarm> userAlarms);
}
