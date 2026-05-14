package com.quertimizer.judge.adapter.in.scheduler;

import com.quertimizer.judge.application.port.in.CleanupOrphanLvmSnapshotsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LvmSnapshotCleanupScheduler {

    private static final long ORPHAN_CLEANUP_INTERVAL_MILLIS = 10 * 60 * 1000L;

    private final CleanupOrphanLvmSnapshotsUseCase cleanupOrphanLvmSnapshots;

    /**
     * 주기적으로 고아 LVM 평가 스냅샷을 정리한다.
     *
     * <ol>
     *   <li>고아 평가 스냅샷 정리 usecase 실행
     * </ol>
     */
    @Scheduled(fixedDelay = ORPHAN_CLEANUP_INTERVAL_MILLIS, initialDelay = ORPHAN_CLEANUP_INTERVAL_MILLIS)
    public void cleanupOrphanLvmSnapshots() {
        cleanupOrphanLvmSnapshots.execute();
    }
}
