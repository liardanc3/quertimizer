package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.adapter.out.persistence.JudgeDatasetDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JudgeDatasetDefinitionJpaRepository extends JpaRepository<JudgeDatasetDefinitionEntity, String> {
}
