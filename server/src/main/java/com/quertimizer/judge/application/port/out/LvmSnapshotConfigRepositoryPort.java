package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.entity.LvmSnapshotConfig;

import java.util.Optional;

public interface LvmSnapshotConfigRepositoryPort {

    Optional<LvmSnapshotConfig> findDefault();
}
