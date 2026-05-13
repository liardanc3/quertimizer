package com.quertimizer.judge.adapter.out.persistence;

import com.quertimizer.judge.application.port.out.LvmSnapshotConfigRepositoryPort;
import com.quertimizer.judge.domain.entity.LvmSnapshotConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LvmSnapshotConfigPersistenceAdapter implements LvmSnapshotConfigRepositoryPort {

    private final LvmSnapshotConfigJpaRepository lvmSnapshotConfigJpaRepository;
    private final LvmSnapshotConfigPersistenceMapper lvmSnapshotConfigPersistenceMapper;

    @Override
    public Optional<LvmSnapshotConfig> findDefault() {
        return lvmSnapshotConfigJpaRepository.findById(LvmSnapshotConfig.DEFAULT_ID)
                .map(lvmSnapshotConfigPersistenceMapper::toDomain);
    }
}
