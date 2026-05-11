package com.quertimizer.monitoring.adapter.out.persistence;

import com.quertimizer.judge.application.port.out.JudgeRuntimeConfigPort;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.JudgeRuntimeConfig;
import com.quertimizer.monitoring.application.port.out.JudgeConfigRepositoryPort;
import com.quertimizer.monitoring.domain.entity.JudgeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JudgeConfigPersistenceAdapter implements JudgeConfigRepositoryPort, JudgeRuntimeConfigPort {

    private final JudgeConfigJpaRepository judgeConfigJpaRepository;
    private final JudgeConfigPersistenceMapper judgeConfigPersistenceMapper;

    @Override
    public List<JudgeConfig> findAll() {
        return judgeConfigJpaRepository.findAllByOrderByDbmsTypeAscDatabaseIdAsc().stream()
                .map(judgeConfigPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<JudgeConfig> findByDatabaseId(String databaseId) {
        return judgeConfigJpaRepository.findById(databaseId)
                .map(judgeConfigPersistenceMapper::toDomain);
    }

    @Override
    public List<JudgeConfig> findByDbmsType(DbmsType dbmsType) {
        return judgeConfigJpaRepository.findAllByDbmsTypeOrderByDatabaseIdAsc(dbmsType).stream()
                .map(judgeConfigPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public JudgeConfig save(JudgeConfig judgeConfig) {
        JudgeConfigJpaEntity entity = judgeConfigJpaRepository.findById(judgeConfig.getDatabaseId())
                .map(storedEntity -> {
                    judgeConfigPersistenceMapper.updateEntity(storedEntity, judgeConfig);
                    return storedEntity;
                })
                .orElseGet(() -> judgeConfigPersistenceMapper.toEntity(judgeConfig));
        return judgeConfigPersistenceMapper.toDomain(judgeConfigJpaRepository.save(entity));
    }

    @Override
    public Optional<JudgeRuntimeConfig> findRuntimeConfig(String databaseId) {
        return findByDatabaseId(databaseId)
                .map(config -> new JudgeRuntimeConfig(config.getDatabaseId(), config.isEnabled(), config.getMaxConcurrency()));
    }
}
