package com.quertimizer.alarm.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAlarmJpaRepository extends JpaRepository<UserAlarmJpaEntity, Long> {
    Page<UserAlarmJpaEntity> findAllByHandleOrderByCreatedAtDescAlarmIdDesc(String handle, Pageable pageable);
    Page<UserAlarmJpaEntity> findAllByHandle(String handle, Pageable pageable);
    Optional<UserAlarmJpaEntity> findByAlarmIdAndHandle(Long alarmId, String handle);
    List<UserAlarmJpaEntity> findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(String handle);
    long countByHandleAndReadFalse(String handle);
}
