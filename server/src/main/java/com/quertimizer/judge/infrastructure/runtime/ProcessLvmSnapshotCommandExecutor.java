package com.quertimizer.judge.infrastructure.runtime;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ProcessLvmSnapshotCommandExecutor implements LvmSnapshotCommandExecutor {

    private final int timeoutSeconds;

    public ProcessLvmSnapshotCommandExecutor(int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }

        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String execute(List<String> command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("LVM runtime command timed out: " + command);
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new IllegalStateException("LVM runtime command failed: " + command + System.lineSeparator() + output);
            }

            return output;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LVM runtime command was interrupted: " + command, exception);
        } catch (Exception exception) {
            throw new IllegalStateException("LVM runtime command failed: " + command, exception);
        }
    }
}
