package com.quertimizer.ranking.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankPageOutput;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingCalculatorTest {

    private final RankingCalculator rankingCalculator = new RankingCalculator();

    @Test
    @DisplayName("성공(스냅샷 계산)")
    void calculateSnapshotSuccess() {
        // given
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        List<RankingSolveRecord> solveRecords = List.of(
                new RankingSolveRecord("P00001-00001", "alpha", DbmsType.POSTGRESQL, 100, 10.0, calculatedAt.minusDays(40)),
                new RankingSolveRecord("P00001-00001", "beta", DbmsType.POSTGRESQL, 100, 20.0, calculatedAt.minusDays(40)),
                new RankingSolveRecord("P00001-00002", "alpha", DbmsType.POSTGRESQL, 100, 30.0, calculatedAt.minusDays(1)),
                new RankingSolveRecord("P00001-00002", "beta", DbmsType.POSTGRESQL, 100, 5.0, calculatedAt.minusDays(1)),
                new RankingSolveRecord("M00001-00001", "mysqlUser", DbmsType.MYSQL, 100, 1.0, calculatedAt.minusDays(1))
        );
        List<RankingSubmitRecord> submitRecords = List.of(
                new RankingSubmitRecord("alpha", DbmsType.POSTGRESQL, true),
                new RankingSubmitRecord("alpha", DbmsType.POSTGRESQL, false),
                new RankingSubmitRecord("beta", DbmsType.POSTGRESQL, true),
                new RankingSubmitRecord("mysqlUser", DbmsType.MYSQL, true)
        );

        // when
        List<RankingSnapshot> snapshots = rankingCalculator.calculateSnapshot(
                solveRecords, submitRecords, DbmsType.POSTGRESQL,
                "snapshot-1", calculatedAt
        );

        // then
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots)
                .extracting(RankingSnapshot::getHandle)
                .containsExactlyInAnyOrder("alpha", "beta");
        assertThat(snapshots)
                .filteredOn(snapshot -> snapshot.getHandle().equals("alpha"))
                .first()
                .satisfies(snapshot -> {
                    assertThat(snapshot.getSolvedCount()).isEqualTo(2);
                    assertThat(snapshot.getAvgExecutionPercentile()).isEqualTo(50.0);
                    assertThat(snapshot.getTotalSubmitCount()).isEqualTo(2);
                    assertThat(snapshot.getSuccessSubmitCount()).isEqualTo(1);
                    assertThat(snapshot.getSolvedCountRank()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("성공(검색 후 고정 rank 유지)")
    void createPageKeepsSnapshotRankWhenFiltered() {
        // given
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        List<RankingSnapshot> snapshots = List.of(
                new RankingSnapshot("snapshot-1", DbmsType.POSTGRESQL, "alpha", 10, 30.0, 20, 10, 20, 30, 0, 0, calculatedAt),
                new RankingSnapshot("snapshot-1", DbmsType.POSTGRESQL, "beta", 8, 20.0, 15, 8, 21, 10, 0, 0, calculatedAt)
        );
        RankSearchInput input = new RankSearchInput(1, 10, "postgresql", "alp", "solvedCount");

        // when
        RankPageOutput page = rankingCalculator.createPage(snapshots, input);

        // then
        assertThat(page.ranks()).hasSize(1);
        assertThat(page.ranks().get(0).handle()).isEqualTo("alpha");
        assertThat(page.ranks().get(0).rank()).isEqualTo(20);
    }
}
