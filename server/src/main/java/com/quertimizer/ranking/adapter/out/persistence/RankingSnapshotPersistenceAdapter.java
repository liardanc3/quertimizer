package com.quertimizer.ranking.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.port.out.RankingSnapshotRepositoryPort;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RankingSnapshotPersistenceAdapter implements RankingSnapshotRepositoryPort {

    private final RankingSnapshotJpaRepository rankingSnapshotJpaRepository;
    private final RankingSnapshotMetaJpaRepository rankingSnapshotMetaJpaRepository;

    @Override
    public Optional<String> findActiveSnapshotId() {
        return rankingSnapshotMetaJpaRepository.findById(RankingSnapshotMetaJpaEntity.ACTIVE_META_ID)
                .map(RankingSnapshotMetaJpaEntity::getActiveSnapshotId);
    }

    @Override
    public List<RankingSnapshot> findAllBySnapshotIdAndDbmsType(String snapshotId, DbmsType dbmsType) {
        return rankingSnapshotJpaRepository.findAllBySnapshotIdAndDbmsType(snapshotId, dbmsType).stream()
                .map(RankingSnapshotJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void replaceActiveSnapshot(String snapshotId, List<RankingSnapshot> snapshots, LocalDateTime calculatedAt) {
        // 새 snapshot record 저장
        rankingSnapshotJpaRepository.saveAll(
                snapshots.stream()
                        .map(RankingSnapshotJpaEntity::from)
                        .toList()
        );

        // active snapshot meta 교체 후 이전 snapshot 제거
        rankingSnapshotMetaJpaRepository.save(RankingSnapshotMetaJpaEntity.active(snapshotId, calculatedAt));
        rankingSnapshotJpaRepository.deleteInactiveSnapshots(snapshotId);
    }
}
