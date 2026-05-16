package com.quertimizer.ranking.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.port.out.RankingProblemRecordPort;
import com.quertimizer.ranking.application.port.out.RankingSnapshotRepositoryPort;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshRankingSnapshotTest {

    @Test
    @DisplayName("성공(snapshot 저장)")
    void executeSuccess() {
        // given
        LocalDateTime submittedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        RankingProblemRecordPort problemRecordPort = new StubProblemRecordPort(
                List.of(
                        new RankingSolveRecord("P00001-00001", "alpha", DbmsType.POSTGRESQL, 100, 10.0, submittedAt),
                        new RankingSolveRecord("M00001-00001", "beta", DbmsType.MYSQL, 100, 10.0, submittedAt)
                ),
                List.of(
                        new RankingSubmitRecord("alpha", DbmsType.POSTGRESQL, true),
                        new RankingSubmitRecord("beta", DbmsType.MYSQL, true)
                )
        );
        CapturingSnapshotRepository snapshotRepository = new CapturingSnapshotRepository();
        RefreshRankingSnapshot refreshRankingSnapshot = new RefreshRankingSnapshot(
                problemRecordPort,
                snapshotRepository,
                new RankingCalculator()
        );

        // when
        refreshRankingSnapshot.execute();

        // then
        assertThat(snapshotRepository.snapshotId).isNotBlank();
        assertThat(snapshotRepository.snapshots)
                .extracting(RankingSnapshot::getDbmsType)
                .containsExactlyInAnyOrder(DbmsType.POSTGRESQL, DbmsType.MYSQL);
    }

    private static class StubProblemRecordPort implements RankingProblemRecordPort {
        private final List<RankingSolveRecord> solveRecords;
        private final List<RankingSubmitRecord> submitRecords;

        private StubProblemRecordPort(List<RankingSolveRecord> solveRecords,
                                      List<RankingSubmitRecord> submitRecords) {
            this.solveRecords = solveRecords;
            this.submitRecords = submitRecords;
        }

        @Override
        public List<RankingSolveRecord> findSolveRecords() {
            return solveRecords;
        }

        @Override
        public List<RankingSubmitRecord> findSubmitRecords() {
            return submitRecords;
        }
    }

    private static class CapturingSnapshotRepository implements RankingSnapshotRepositoryPort {
        private String snapshotId;
        private List<RankingSnapshot> snapshots = List.of();

        @Override
        public Optional<String> findActiveSnapshotId() {
            return Optional.empty();
        }

        @Override
        public List<RankingSnapshot> findAllBySnapshotIdAndDbmsType(String snapshotId, DbmsType dbmsType) {
            return List.of();
        }

        @Override
        public void replaceActiveSnapshot(String snapshotId, List<RankingSnapshot> snapshots, LocalDateTime calculatedAt) {
            this.snapshotId = snapshotId;
            this.snapshots = List.copyOf(snapshots);
        }
    }
}
