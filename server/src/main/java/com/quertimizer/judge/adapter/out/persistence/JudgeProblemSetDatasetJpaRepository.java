package com.quertimizer.judge.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JudgeProblemSetDatasetJpaRepository extends JpaRepository<JudgeProblemSetDatasetEntity, Long> {

    Optional<JudgeProblemSetDatasetEntity> findByDatasetId(String datasetId);
}
