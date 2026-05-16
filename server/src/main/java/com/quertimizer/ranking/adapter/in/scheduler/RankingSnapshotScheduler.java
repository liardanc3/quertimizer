package com.quertimizer.ranking.adapter.in.scheduler;

import com.quertimizer.ranking.application.port.in.RefreshRankingSnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingSnapshotScheduler {

    private static final long RANKING_SNAPSHOT_INTERVAL_MILLIS = 60_000L;
    private static final long RANKING_SNAPSHOT_INITIAL_DELAY_MILLIS = 10_000L;

    private final RefreshRankingSnapshotUseCase refreshRankingSnapshot;

    /**
     * 주기적으로 랭킹 스냅샷을 갱신한다.
     *
     * <ol>
     *   <li>랭킹 스냅샷 갱신 usecase 실행
     * </ol>
     */
    @Scheduled(fixedDelay = RANKING_SNAPSHOT_INTERVAL_MILLIS, initialDelay = RANKING_SNAPSHOT_INITIAL_DELAY_MILLIS)
    public void refreshRankingSnapshot() {
        refreshRankingSnapshot.execute();
    }
}
