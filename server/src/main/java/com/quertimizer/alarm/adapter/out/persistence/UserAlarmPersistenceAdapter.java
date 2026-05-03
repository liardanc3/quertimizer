package com.quertimizer.alarm.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.alarm.application.port.out.UserAlarmRepositoryPort;
import com.quertimizer.alarm.domain.entity.UserAlarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAlarmPersistenceAdapter implements UserAlarmRepositoryPort {

    private final UserAlarmJpaRepository userAlarmJpaRepository;
    private final UserAlarmPersistenceMapper userAlarmPersistenceMapper;

    @Override
    public Page<UserAlarm> findAllByHandleOrderByCreatedAtDescAlarmIdDesc(String handle, Pageable pageable) {
        return userAlarmJpaRepository.findAllByHandleOrderByCreatedAtDescAlarmIdDesc(handle, pageable)
                .map(userAlarmPersistenceMapper::toDomain);
    }

    @Override
    public Page<UserAlarm> findAllByHandle(String handle, Pageable pageable) {
        return userAlarmJpaRepository.findAllByHandle(handle, pageable)
                .map(userAlarmPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UserAlarm> findByAlarmIdAndHandle(Long alarmId, String handle) {
        return userAlarmJpaRepository.findByAlarmIdAndHandle(alarmId, handle)
                .map(userAlarmPersistenceMapper::toDomain);
    }

    @Override
    public List<UserAlarm> findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(String handle) {
        return userAlarmJpaRepository.findAllByHandleAndReadFalseOrderByCreatedAtDescAlarmIdDesc(handle).stream()
                .map(userAlarmPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByHandleAndReadFalse(String handle) {
        return userAlarmJpaRepository.countByHandleAndReadFalse(handle);
    }

    @Override
    public UserAlarm save(UserAlarm userAlarm) {
        UserAlarmJpaEntity savedEntity = userAlarm.getAlarmId() == null
                ? userAlarmPersistenceMapper.toEntity(userAlarm)
                : userAlarmJpaRepository.findById(userAlarm.getAlarmId())
                        .map(entity -> {
                            userAlarmPersistenceMapper.updateEntity(entity, userAlarm);
                            return entity;
                        })
                        .orElseGet(() -> userAlarmPersistenceMapper.toEntity(userAlarm));
        return userAlarmPersistenceMapper.toDomain(userAlarmJpaRepository.save(savedEntity));
    }

    @Override
    public List<UserAlarm> saveAll(Iterable<UserAlarm> userAlarms) {
        List<UserAlarm> savedUserAlarms = new java.util.ArrayList<>();
        userAlarms.forEach(userAlarm -> savedUserAlarms.add(save(userAlarm)));
        return savedUserAlarms;
    }
}
