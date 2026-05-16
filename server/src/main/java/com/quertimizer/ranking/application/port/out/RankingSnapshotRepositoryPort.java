package com.quertimizer.ranking.application.port.out;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.domain.model.RankingSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RankingSnapshotRepositoryPort {

    Optional<String> findActiveSnapshotId();

    List<RankingSnapshot> findAllBySnapshotIdAndDbmsType(String snapshotId, DbmsType dbmsType);

    void replaceActiveSnapshot(String snapshotId, List<RankingSnapshot> snapshots, LocalDateTime calculatedAt);
}
