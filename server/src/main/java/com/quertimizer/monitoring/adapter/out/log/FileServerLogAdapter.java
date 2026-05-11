package com.quertimizer.monitoring.adapter.out.log;

import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;
import com.quertimizer.monitoring.application.port.out.ServerLogPort;
import com.quertimizer.monitoring.domain.model.MonitoringLogLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
public class FileServerLogAdapter implements ServerLogPort {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Path logBasePath;

    public FileServerLogAdapter(@Value("${monitoring.logs.base-dir:/home/quertimizer/apps/server/logs}") String logBaseDir) {
        this.logBasePath = Path.of(logBaseDir).normalize();
    }

    @Override
    public ServerLogOutput readLogs(MonitoringLogSearchInput input) {
        // 요청 레벨과 날짜에 맞는 로그 파일 후보 조회
        List<Path> candidates = resolveLogFileCandidates(input);
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return new ServerLogOutput(input.getLevel(), input.getDate(), true, tail(candidate, input.getSize()));
            }
        }

        return new ServerLogOutput(input.getLevel(), input.getDate(), false, List.of());
    }

    private List<Path> resolveLogFileCandidates(MonitoringLogSearchInput input) {
        // logback rolling path 규칙에 맞는 파일 후보 생성
        String date = input.getDate().format(DATE_FORMATTER);
        if (input.getLevel() == MonitoringLogLevel.WARN) {
            Path warnDir = logBasePath.resolve(input.getLevel().getValue()).resolve(input.getDate().format(MONTH_FORMATTER));
            return List.of(warnDir.resolve(date + ".log"), warnDir.resolve(date + ".log.gz"));
        }

        Path levelDir = logBasePath.resolve(input.getLevel().getValue());
        return List.of(levelDir.resolve(date + ".log"), levelDir.resolve(date + ".log.gz"));
    }

    private List<String> tail(Path path, int size) {
        // 전체 로그 중 마지막 size줄만 유지
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = openReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (lines.size() > size) {
                    lines.remove(0);
                }
            }
        } catch (IOException ignored) {
            return List.of();
        }

        return List.copyOf(lines);
    }

    private BufferedReader openReader(Path path) throws IOException {
        // 압축 로그와 일반 로그 reader 생성
        if (path.getFileName().toString().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8));
        }

        return Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }
}
