package com.quertimizer.monitoring.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JudgeConfigJpaRepository extends JpaRepository<JudgeConfigJpaEntity, String> {

    List<JudgeConfigJpaEntity> findAllByOrderByDbmsTypeAscDatabaseIdAsc();

    List<JudgeConfigJpaEntity> findAllByDbmsTypeOrderByDatabaseIdAsc(DbmsType dbmsType);
}
