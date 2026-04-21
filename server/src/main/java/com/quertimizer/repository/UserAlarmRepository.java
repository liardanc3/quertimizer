package com.quertimizer.repository;

import com.quertimizer.entity.UserAlarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    Page<UserAlarm> findAllByUserIdOrderByCreatedAtDescAlarmIdDesc(String userId, Pageable pageable);

    Optional<UserAlarm> findByAlarmIdAndUserId(Long alarmId, String userId);

    List<UserAlarm> findAllByUserIdAndReadFalseOrderByCreatedAtDescAlarmIdDesc(String userId);

    long countByUserIdAndReadFalse(String userId);

}
