package com.quertimizer.judge.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.judge.application.port.in.CleanupOrphanLvmSnapshotsUseCase;
import com.quertimizer.judge.application.port.out.OrphanLvmSnapshotCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CleanupOrphanLvmSnapshots implements CleanupOrphanLvmSnapshotsUseCase {

    private final OrphanLvmSnapshotCleanupPort orphanLvmSnapshotCleanupPort;

    /**
     * 고아 LVM 평가 스냅샷을 정리한다.
     */
    @Override
    @Log("고아 리소스 정리")
    public void execute() {
        orphanLvmSnapshotCleanupPort.cleanupOrphans();
    }
}
