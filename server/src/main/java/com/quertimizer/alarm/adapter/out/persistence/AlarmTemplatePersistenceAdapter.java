package com.quertimizer.alarm.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import com.quertimizer.alarm.application.port.out.AlarmTemplateRepositoryPort;
import com.quertimizer.alarm.domain.entity.AlarmTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmTemplatePersistenceAdapter implements AlarmTemplateRepositoryPort {

    private final AlarmTemplateJpaRepository alarmTemplateJpaRepository;
    private final AlarmTemplatePersistenceMapper alarmTemplatePersistenceMapper;

    @Override
    public List<AlarmTemplate> findAllByOrderByAlarmTypeAsc() {
        return alarmTemplateJpaRepository.findAllByOrderByAlarmTypeAsc().stream()
                .map(alarmTemplatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AlarmTemplate> findById(String alarmType) {
        return alarmTemplateJpaRepository.findById(alarmType)
                .map(alarmTemplatePersistenceMapper::toDomain);
    }

    @Override
    public AlarmTemplate save(AlarmTemplate alarmTemplate) {
        AlarmTemplateJpaEntity savedEntity = alarmTemplateJpaRepository.findById(alarmTemplate.getAlarmType())
                        .map(entity -> {
                            alarmTemplatePersistenceMapper.updateEntity(entity, alarmTemplate);
                            return entity;
                        })
                        .orElseGet(() -> alarmTemplatePersistenceMapper.toEntity(alarmTemplate));

        return alarmTemplatePersistenceMapper.toDomain(alarmTemplateJpaRepository.save(savedEntity));
    }
}
