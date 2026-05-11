package com.quertimizer.judge.adapter.out.execution;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ProcessLvmSnapshotCommandExecutor implements LvmSnapshotCommandExecutor {

    private final int timeoutSeconds;

    public ProcessLvmSnapshotCommandExecutor(int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("LVM 명령 제한 시간은 0보다 커야 합니다.");
        }

        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String execute(List<String> command) {
        Objects.requireNonNull(command, "실행할 LVM 명령이 필요합니다.");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("실행할 LVM 명령이 비어 있습니다.");
        }

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("LVM 런타임 명령 시간이 초과되었습니다: " + command);
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("LVM 런타임 명령 실패: " + command + System.lineSeparator() + output);
            }

            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LVM 런타임 명령이 중단되었습니다: " + command, exception);
        } catch (Exception exception) {
            throw new IllegalStateException("LVM 런타임 명령 실패: " + command, exception);
        }
    }
}
