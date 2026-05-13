package com.quertimizer.monitoring.adapter.out.micrometer;

import com.quertimizer.monitoring.application.output.SystemResourceOutput;
import com.quertimizer.monitoring.application.port.out.SystemResourcePort;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MicrometerSystemResourceAdapter implements SystemResourcePort {

    private final MeterRegistry meterRegistry;
    private CpuSnapshot previousCpuSnapshot;

    @PostConstruct
    public void initializeCpuSnapshot() {
        // VM 전체 CPU 사용률 첫 조회 기준점 준비
        previousCpuSnapshot = readCpuSnapshot();
    }

    @Override
    public SystemResourceOutput getSystemResources() {
        // VM 전체 CPU와 JVM 프로세스 자원 수집
        com.sun.management.OperatingSystemMXBean operatingSystem =
                ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
        double systemCpuUsage = readVmCpuUsage(operatingSystem != null ? operatingSystem.getCpuLoad() : readGauge("system.cpu.usage", 0.0));
        double processCpuUsage = readGauge("process.cpu.usage", operatingSystem != null ? operatingSystem.getProcessCpuLoad() : 0.0);
        long totalMemoryBytes = operatingSystem != null ? operatingSystem.getTotalMemorySize() : Runtime.getRuntime().maxMemory();
        long freeMemoryBytes = operatingSystem != null ? operatingSystem.getFreeMemorySize() : Runtime.getRuntime().freeMemory();
        DiskUsage diskUsage = readDiskUsage();

        return new SystemResourceOutput(
                toPercent(systemCpuUsage), toPercent(processCpuUsage), ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage(),
                totalMemoryBytes, Math.max(0, totalMemoryBytes - freeMemoryBytes),
                diskUsage.totalBytes, diskUsage.usedBytes,
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000
        );
    }

    private synchronized double readVmCpuUsage(double fallback) {
        // /proc/stat 기준 VM 전체 CPU 사용률 계산
        CpuSnapshot currentCpuSnapshot = readCpuSnapshot();
        if (currentCpuSnapshot == null) {
            return Math.max(0, fallback);
        }

        // 이전 기준점 없으면 현재 값을 기준점으로 저장 후 fallback 사용
        if (previousCpuSnapshot == null) {
            previousCpuSnapshot = currentCpuSnapshot;
            return Math.max(0, fallback);
        }

        // 전체 CPU 누적값 변화량으로 사용률 계산
        long totalDelta = currentCpuSnapshot.totalTicks - previousCpuSnapshot.totalTicks;
        long idleDelta = currentCpuSnapshot.idleTicks - previousCpuSnapshot.idleTicks;
        previousCpuSnapshot = currentCpuSnapshot;
        if (totalDelta <= 0) {
            return Math.max(0, fallback);
        }

        return Math.max(0.0, Math.min(1.0, (double) (totalDelta - idleDelta) / totalDelta));
    }

    private CpuSnapshot readCpuSnapshot() {
        // Linux VM 전체 CPU 누적 카운터 조회
        try {
            String cpuLine = Files.readAllLines(Path.of("/proc/stat")).stream()
                    .filter(line -> line.startsWith("cpu "))
                    .findFirst()
                    .orElse("");
            if (cpuLine.isBlank()) {
                return null;
            }

            // idle/iowait과 전체 tick 합산
            long[] ticks = Arrays.stream(cpuLine.trim().split("\\s+"))
                    .skip(1)
                    .mapToLong(Long::parseLong)
                    .toArray();
            if (ticks.length < 5) {
                return null;
            }

            long idleTicks = ticks[3] + ticks[4];
            long totalTicks = Arrays.stream(ticks).sum();
            return new CpuSnapshot(totalTicks, idleTicks);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double readGauge(String name, double fallback) {
        // micrometer gauge 값 조회 후 없거나 비정상이면 fallback 사용
        Double value = meterRegistry.find(name).gauge() != null ? meterRegistry.find(name).gauge().value() : null;
        if (value == null || value.isNaN() || value.isInfinite() || value < 0) {
            return Math.max(0, fallback);
        }

        return value;
    }

    private double toPercent(double ratio) {
        // 비율 값을 percent로 변환
        return Math.max(0.0, Math.min(100.0, ratio * 100.0));
    }

    private DiskUsage readDiskUsage() {
        // 파일스토어 전체 기준 디스크 사용량 합산
        long totalBytes = 0L;
        long usableBytes = 0L;
        for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
            try {
                totalBytes += Math.max(0L, fileStore.getTotalSpace());
                usableBytes += Math.max(0L, fileStore.getUsableSpace());
            } catch (IOException ignored) {
            }
        }

        return new DiskUsage(totalBytes, Math.max(0L, totalBytes - usableBytes));
    }

    private static final class DiskUsage {
        private final long totalBytes;
        private final long usedBytes;

        private DiskUsage(long totalBytes, long usedBytes) {
            this.totalBytes = totalBytes;
            this.usedBytes = usedBytes;
        }
    }

    private static final class CpuSnapshot {
        private final long totalTicks;
        private final long idleTicks;

        private CpuSnapshot(long totalTicks, long idleTicks) {
            this.totalTicks = totalTicks;
            this.idleTicks = idleTicks;
        }
    }
}
