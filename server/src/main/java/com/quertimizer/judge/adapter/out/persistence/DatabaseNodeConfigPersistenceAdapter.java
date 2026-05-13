package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.application.port.out.DatabaseNodeConfigRepositoryPort;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatabaseNodeConfigPersistenceAdapter implements DatabaseNodeConfigRepositoryPort {

    private final DatabaseNodeConfigJpaRepository databaseNodeConfigJpaRepository;
    private final DatabaseNodeConfigPersistenceMapper databaseNodeConfigPersistenceMapper;

    @Override
    public List<DatabaseNodeConfig> findAll() {
        return databaseNodeConfigJpaRepository.findAllByOrderByDbmsTypeAscDatabaseIdAsc().stream()
                .map(databaseNodeConfigPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DatabaseNodeConfig> findByDatabaseId(String databaseId) {
        return databaseNodeConfigJpaRepository.findById(databaseId)
                .map(databaseNodeConfigPersistenceMapper::toDomain);
    }

    @Override
    public List<DatabaseNodeConfig> findByDbmsType(DbmsType dbmsType) {
        return databaseNodeConfigJpaRepository.findAllByDbmsTypeOrderByDatabaseIdAsc(dbmsType).stream()
                .map(databaseNodeConfigPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public DatabaseNodeConfig save(DatabaseNodeConfig databaseNodeConfig) {
        // 기존 설정이 있으면 갱신하고 없으면 신규 엔티티 생성
        DatabaseNodeConfigJpaEntity entity = databaseNodeConfigJpaRepository.findById(databaseNodeConfig.getDatabaseId())
                .map(storedEntity -> {
                    databaseNodeConfigPersistenceMapper.updateEntity(storedEntity, databaseNodeConfig);
                    return storedEntity;
                })
                .orElseGet(() -> databaseNodeConfigPersistenceMapper.toEntity(databaseNodeConfig));
        return databaseNodeConfigPersistenceMapper.toDomain(databaseNodeConfigJpaRepository.save(entity));
    }
}
