package com.quertimizer.judge.adapter.in.scheduler;

import com.quertimizer.judge.application.port.in.CleanupOrphanLvmSnapshotsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("LvmSnapshotCleanupScheduler")
class LvmSnapshotCleanupSchedulerIntegrationTest {

    @Autowired private LvmSnapshotCleanupScheduler lvmSnapshotCleanupScheduler;

    @MockitoBean private CleanupOrphanLvmSnapshotsUseCase cleanupOrphanLvmSnapshots;

    @Nested
    @DisplayName("Scheduled cleanupOrphanLvmSnapshots")
    class CleanupOrphanLvmSnapshots {

        @Test
        @DisplayName("성공 (고아 LVM 스냅샷 정리)")
        void successWhenCleanupTriggered() {
            // given

            // when
            lvmSnapshotCleanupScheduler.cleanupOrphanLvmSnapshots();

            // then
            verify(cleanupOrphanLvmSnapshots).execute();
        }
    }
}
