package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import org.springframework.stereotype.Component;

@Component
public class DatabaseNodeConfigPersistenceMapper {

    public DatabaseNodeConfigJpaEntity toEntity(DatabaseNodeConfig databaseNodeConfig) {
        // 도메인 설정을 JPA 엔티티로 변환
        return DatabaseNodeConfigJpaEntity.create(
                databaseNodeConfig.getDatabaseId(), databaseNodeConfig.getDatabaseName(), databaseNodeConfig.getDbmsType(),
                databaseNodeConfig.getUrlPropertyKey(), databaseNodeConfig.getUsernamePropertyKey(), databaseNodeConfig.getPasswordPropertyKey(),
                databaseNodeConfig.getContainerName(), databaseNodeConfig.getHost(),
                databaseNodeConfig.getPortStart(), databaseNodeConfig.getPortEnd(),
                databaseNodeConfig.getProcessDatabaseName(), databaseNodeConfig.getRootPasswordPropertyKey(),
                databaseNodeConfig.isEnabled(), databaseNodeConfig.getMaxConcurrency(), databaseNodeConfig.getUpdatedAt()
        );
    }

    public DatabaseNodeConfig toDomain(DatabaseNodeConfigJpaEntity entity) {
        // JPA 엔티티를 도메인 설정으로 변환
        return DatabaseNodeConfig.restore(
                entity.getDatabaseId(), entity.getDatabaseName(), entity.getDbmsType(),
                entity.getUrlPropertyKey(), entity.getUsernamePropertyKey(), entity.getPasswordPropertyKey(),
                entity.getContainerName(), entity.getHost(),
                entity.getPortStart(), entity.getPortEnd(),
                entity.getProcessDatabaseName(), entity.getRootPasswordPropertyKey(),
                entity.isEnabled(), entity.getMaxConcurrency(), entity.getUpdatedAt()
        );
    }

    public void updateEntity(DatabaseNodeConfigJpaEntity entity, DatabaseNodeConfig databaseNodeConfig) {
        // 도메인 설정 값으로 기존 JPA 엔티티 갱신
        entity.update(
                databaseNodeConfig.getDatabaseName(), databaseNodeConfig.getDbmsType(),
                databaseNodeConfig.getUrlPropertyKey(), databaseNodeConfig.getUsernamePropertyKey(),
                databaseNodeConfig.getPasswordPropertyKey(), databaseNodeConfig.getContainerName(),
                databaseNodeConfig.getHost(), databaseNodeConfig.getPortStart(),
                databaseNodeConfig.getPortEnd(), databaseNodeConfig.getProcessDatabaseName(),
                databaseNodeConfig.getRootPasswordPropertyKey(),
                databaseNodeConfig.isEnabled(), databaseNodeConfig.getMaxConcurrency(), databaseNodeConfig.getUpdatedAt()
        );
    }
}
