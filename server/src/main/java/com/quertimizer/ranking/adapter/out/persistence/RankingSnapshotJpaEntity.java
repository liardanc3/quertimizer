package com.quertimizer.ranking.adapter.out.persistence;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@IdClass(RankingSnapshotJpaId.class)
@Table(
        name = "ranking_snapshot",
        indexes = {
                @Index(name = "idx_ranking_snapshot_solved_rank", columnList = "snapshot_id, dbms_type, solved_count_rank"),
                @Index(name = "idx_ranking_snapshot_percentile_rank", columnList = "snapshot_id, dbms_type, avg_execution_percentile_rank"),
                @Index(name = "idx_ranking_snapshot_handle", columnList = "snapshot_id, dbms_type, handle")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingSnapshotJpaEntity {

    @Id
    @Column(name = "snapshot_id", nullable = false, length = 36)
    private String snapshotId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", nullable = false, length = 20)
    private DbmsType dbmsType;

    @Id
    @Column(nullable = false, length = 50)
    private String handle;

    @Column(name = "solved_count", nullable = false)
    private int solvedCount;

    @Column(name = "avg_execution_percentile", nullable = false)
    private double avgExecutionPercentile;

    @Column(name = "total_submit_count", nullable = false)
    private int totalSubmitCount;

    @Column(name = "success_submit_count", nullable = false)
    private int successSubmitCount;

    @Column(name = "solved_count_rank", nullable = false)
    private int solvedCountRank;

    @Column(name = "avg_execution_percentile_rank", nullable = false)
    private int avgExecutionPercentileRank;

    @Column(name = "solved_count_rank_delta", nullable = false)
    private int solvedCountRankDelta;

    @Column(name = "avg_execution_percentile_rank_delta", nullable = false)
    private int avgExecutionPercentileRankDelta;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    public static RankingSnapshotJpaEntity from(RankingSnapshot snapshot) {
        // 도메인 snapshot을 JPA 엔티티로 변환
        return new RankingSnapshotJpaEntity(
                snapshot.getSnapshotId(), snapshot.getDbmsType(), snapshot.getHandle(),
                snapshot.getSolvedCount(), snapshot.getAvgExecutionPercentile(),
                snapshot.getTotalSubmitCount(), snapshot.getSuccessSubmitCount(),
                snapshot.getSolvedCountRank(), snapshot.getAvgExecutionPercentileRank(),
                snapshot.getSolvedCountRankDelta(), snapshot.getAvgExecutionPercentileRankDelta(),
                snapshot.getCalculatedAt()
        );
    }

    public RankingSnapshot toDomain() {
        // JPA 엔티티를 도메인 snapshot으로 변환
        return new RankingSnapshot(
                snapshotId, dbmsType, handle,
                solvedCount, avgExecutionPercentile,
                totalSubmitCount, successSubmitCount,
                solvedCountRank, avgExecutionPercentileRank,
                solvedCountRankDelta, avgExecutionPercentileRankDelta,
                calculatedAt
        );
    }

    private RankingSnapshotJpaEntity(String snapshotId, DbmsType dbmsType, String handle,
                                     int solvedCount, double avgExecutionPercentile,
                                     int totalSubmitCount, int successSubmitCount,
                                     int solvedCountRank, int avgExecutionPercentileRank,
                                     int solvedCountRankDelta, int avgExecutionPercentileRankDelta,
                                     LocalDateTime calculatedAt) {
        this.snapshotId = snapshotId;
        this.dbmsType = dbmsType;
        this.handle = handle;
        this.solvedCount = solvedCount;
        this.avgExecutionPercentile = avgExecutionPercentile;
        this.totalSubmitCount = totalSubmitCount;
        this.successSubmitCount = successSubmitCount;
        this.solvedCountRank = solvedCountRank;
        this.avgExecutionPercentileRank = avgExecutionPercentileRank;
        this.solvedCountRankDelta = solvedCountRankDelta;
        this.avgExecutionPercentileRankDelta = avgExecutionPercentileRankDelta;
        this.calculatedAt = calculatedAt;
    }
}
