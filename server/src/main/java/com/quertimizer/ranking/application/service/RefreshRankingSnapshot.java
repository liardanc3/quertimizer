package com.quertimizer.ranking.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.port.in.RefreshRankingSnapshotUseCase;
import com.quertimizer.ranking.application.port.out.RankingProblemRecordPort;
import com.quertimizer.ranking.application.port.out.RankingSnapshotRepositoryPort;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshRankingSnapshot implements RefreshRankingSnapshotUseCase {

    private final RankingProblemRecordPort rankingProblemRecordPort;
    private final RankingSnapshotRepositoryPort rankingSnapshotRepository;
    private final RankingCalculator rankingCalculator;

    /**
     * 현재 제출 기록 기준 랭킹 스냅샷을 새로 생성한다.
     *
     * <ol>
     *   <li>랭킹 계산 원천 기록 조회
     *   <li>DBMS별 랭킹 스냅샷 계산
     *   <li>활성 스냅샷 교체
     * </ol>
     */
    @Override
    @Transactional
    public void execute() {
        // 랭킹 계산 원천 기록과 새 snapshot 식별자 확정
        String snapshotId = UUID.randomUUID().toString();
        LocalDateTime calculatedAt = LocalDateTime.now();
        List<RankingSolveRecord> solveRecords = rankingProblemRecordPort.findSolveRecords();
        List<RankingSubmitRecord> submitRecords = rankingProblemRecordPort.findSubmitRecords();

        // 지원 DBMS별 snapshot record 생성
        List<RankingSnapshot> snapshots = new ArrayList<>();
        for (DbmsType dbmsType : DbmsType.values()) {
            snapshots.addAll(rankingCalculator.calculateSnapshot(
                    solveRecords, submitRecords, dbmsType,
                    snapshotId, calculatedAt
            ));
        }

        // 새 snapshot 활성화와 이전 snapshot 정리
        rankingSnapshotRepository.replaceActiveSnapshot(snapshotId, snapshots, calculatedAt);
    }
}
