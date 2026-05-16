package com.quertimizer.ranking.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ranking_snapshot_meta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingSnapshotMetaJpaEntity {

    static final String ACTIVE_META_ID = "ACTIVE";

    @Id
    @Column(name = "meta_id", nullable = false, length = 20)
    private String metaId;

    @Column(name = "active_snapshot_id", nullable = false, length = 36)
    private String activeSnapshotId;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    static RankingSnapshotMetaJpaEntity active(String snapshotId, LocalDateTime calculatedAt) {
        // 활성 snapshot meta 엔티티 생성
        return new RankingSnapshotMetaJpaEntity(ACTIVE_META_ID, snapshotId, calculatedAt);
    }

    private RankingSnapshotMetaJpaEntity(String metaId, String activeSnapshotId, LocalDateTime calculatedAt) {
        this.metaId = metaId;
        this.activeSnapshotId = activeSnapshotId;
        this.calculatedAt = calculatedAt;
    }
}
