package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatabaseNodeConfigJpaRepository extends JpaRepository<DatabaseNodeConfigJpaEntity, String> {

    List<DatabaseNodeConfigJpaEntity> findAllByOrderByDbmsTypeAscDatabaseIdAsc();

    List<DatabaseNodeConfigJpaEntity> findAllByDbmsTypeOrderByDatabaseIdAsc(DbmsType dbmsType);
}
