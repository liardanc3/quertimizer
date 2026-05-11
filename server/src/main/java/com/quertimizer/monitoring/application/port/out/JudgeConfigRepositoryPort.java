package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.monitoring.domain.entity.JudgeConfig;

import java.util.List;
import java.util.Optional;

public interface JudgeConfigRepositoryPort {

    List<JudgeConfig> findAll();

    Optional<JudgeConfig> findByDatabaseId(String databaseId);

    List<JudgeConfig> findByDbmsType(DbmsType dbmsType);

    JudgeConfig save(JudgeConfig judgeConfig);
}
