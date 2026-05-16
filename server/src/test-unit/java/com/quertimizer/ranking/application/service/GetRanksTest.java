package com.quertimizer.ranking.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankPageOutput;
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

class GetRanksTest {

    @Test
    @DisplayName("성공(snapshot 기준 조회)")
    void executeWithActiveSnapshotSuccess() {
        // given
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        RankingSnapshotRepositoryPort snapshotRepository = new StubSnapshotRepository(
                Optional.of("snapshot-1"),
                List.of(new RankingSnapshot("snapshot-1", DbmsType.POSTGRESQL, "alpha", 10, 30.0, 20, 10, 7, 9, 0, 0, calculatedAt))
        );
        GetRanks getRanks = new GetRanks(new ThrowingProblemRecordPort(), snapshotRepository, new RankingCalculator());
        RankSearchInput input = new RankSearchInput(1, 10, "postgresql", "alp", "solvedCount");

        // when
        RankPageOutput output = getRanks.execute(input);

        // then
        assertThat(output.ranks()).hasSize(1);
        assertThat(output.ranks().get(0).rank()).isEqualTo(7);
        assertThat(output.ranks().get(0).handle()).isEqualTo("alpha");
    }

    @Test
    @DisplayName("성공(snapshot 미존재 fallback)")
    void executeFallbackSuccess() {
        // given
        LocalDateTime submittedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        RankingProblemRecordPort problemRecordPort = new StubProblemRecordPort(
                List.of(new RankingSolveRecord("P00001-00001", "alpha", DbmsType.POSTGRESQL, 100, 10.0, submittedAt)),
                List.of(new RankingSubmitRecord("alpha", DbmsType.POSTGRESQL, true))
        );
        RankingSnapshotRepositoryPort snapshotRepository = new StubSnapshotRepository(Optional.empty(), List.of());
        GetRanks getRanks = new GetRanks(problemRecordPort, snapshotRepository, new RankingCalculator());
        RankSearchInput input = new RankSearchInput(1, 10, "postgresql", null, "solvedCount");

        // when
        RankPageOutput output = getRanks.execute(input);

        // then
        assertThat(output.ranks()).hasSize(1);
        assertThat(output.ranks().get(0).handle()).isEqualTo("alpha");
        assertThat(output.ranks().get(0).rank()).isEqualTo(1);
    }

    private static class StubSnapshotRepository implements RankingSnapshotRepositoryPort {
        private final Optional<String> activeSnapshotId;
        private final List<RankingSnapshot> snapshots;

        private StubSnapshotRepository(Optional<String> activeSnapshotId, List<RankingSnapshot> snapshots) {
            this.activeSnapshotId = activeSnapshotId;
            this.snapshots = snapshots;
        }

        @Override
        public Optional<String> findActiveSnapshotId() {
            return activeSnapshotId;
        }

        @Override
        public List<RankingSnapshot> findAllBySnapshotIdAndDbmsType(String snapshotId, DbmsType dbmsType) {
            return snapshots.stream()
                    .filter(snapshot -> snapshot.getSnapshotId().equals(snapshotId))
                    .filter(snapshot -> snapshot.getDbmsType() == dbmsType)
                    .toList();
        }

        @Override
        public void replaceActiveSnapshot(String snapshotId, List<RankingSnapshot> snapshots, LocalDateTime calculatedAt) {
            throw new UnsupportedOperationException();
        }
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

    private static class ThrowingProblemRecordPort implements RankingProblemRecordPort {

        @Override
        public List<RankingSolveRecord> findSolveRecords() {
            throw new AssertionError("snapshot 조회에서는 원천 기록을 조회하지 않아야 함");
        }

        @Override
        public List<RankingSubmitRecord> findSubmitRecords() {
            throw new AssertionError("snapshot 조회에서는 원천 기록을 조회하지 않아야 함");
        }
    }
}
