package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;

import java.util.List;
import java.util.Optional;

public interface DatabaseNodeConfigRepositoryPort {

    List<DatabaseNodeConfig> findAll();

    Optional<DatabaseNodeConfig> findByDatabaseId(String databaseId);

    List<DatabaseNodeConfig> findByDbmsType(DbmsType dbmsType);

    DatabaseNodeConfig save(DatabaseNodeConfig databaseNodeConfig);
}
