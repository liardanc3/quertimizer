package com.quertimizer.ranking.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankPageOutput;
import com.quertimizer.ranking.application.port.in.GetRanksUseCase;
import com.quertimizer.ranking.application.port.out.RankingProblemRecordPort;
import com.quertimizer.ranking.application.port.out.RankingSnapshotRepositoryPort;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRanks implements GetRanksUseCase {

    private static final String FALLBACK_SNAPSHOT_ID = "fallback";

    private final RankingProblemRecordPort rankingProblemRecordPort;
    private final RankingSnapshotRepositoryPort rankingSnapshotRepository;
    private final RankingCalculator rankingCalculator;

    /**
     * 랭킹 검색 입력에 맞는 사용자 랭킹 페이지를 생성한다.
     *
     * <ol>
     *   <li>DBMS와 활성 스냅샷 조회
     *   <li>스냅샷 기반 랭킹 페이지 생성
     *   <li>스냅샷 미존재 시 실시간 계산 결과 반환
     * </ol>
     *
     * @param input 랭킹 검색 조건
     */
    @Override
    @Log("랭킹 목록 조회")
    public RankPageOutput execute(RankSearchInput input) {
        // DBMS와 active snapshot 기준 조회
        DbmsType dbmsType = DbmsType.fromValueOrDefault(input.getDbms(), DbmsType.POSTGRESQL);
        return rankingSnapshotRepository.findActiveSnapshotId()
                .map(snapshotId -> rankingCalculator.createPage(
                        rankingSnapshotRepository.findAllBySnapshotIdAndDbmsType(snapshotId, dbmsType),
                        input
                ))
                .orElseGet(() -> createFallbackPage(input, dbmsType));
    }

    private RankPageOutput createFallbackPage(RankSearchInput input, DbmsType dbmsType) {
        // 초기 snapshot 생성 전 기존 방식으로 랭킹 페이지 계산
        LocalDateTime calculatedAt = LocalDateTime.now();
        List<RankingSolveRecord> solveRecords = rankingProblemRecordPort.findSolveRecords();
        List<RankingSubmitRecord> submitRecords = rankingProblemRecordPort.findSubmitRecords();
        List<RankingSnapshot> snapshots = rankingCalculator.calculateSnapshot(
                solveRecords, submitRecords, dbmsType,
                FALLBACK_SNAPSHOT_ID, calculatedAt
        );

        return rankingCalculator.createPage(snapshots, input);
    }
}
