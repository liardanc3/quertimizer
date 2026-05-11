package com.quertimizer.judge.adapter.out.execution;

public class LvmSnapshotRuntimeNode {

    private final String databaseId;
    private final String runnerContainer;
    private final String host;
    private final int portStart;
    private final int portEnd;
    private final String databaseName;
    private final String rootPassword;

    public LvmSnapshotRuntimeNode(String databaseId, String runnerContainer, String host,
                                  int portStart, int portEnd,
                                  String databaseName, String rootPassword) {
        this.databaseId = requireText(databaseId, "databaseId");
        this.runnerContainer = requireText(runnerContainer, "runnerContainer");
        this.host = requireText(host, "host");
        if (portStart <= 0 || portEnd < portStart) {
            throw new IllegalArgumentException("포트 범위는 0보다 크고 순서가 맞아야 합니다.");
        }

        this.portStart = portStart;
        this.portEnd = portEnd;
        this.databaseName = databaseName != null ? databaseName.trim() : "";
        this.rootPassword = rootPassword != null ? rootPassword : "";
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getRunnerContainer() {
        return runnerContainer;
    }

    public String getHost() {
        return host;
    }

    public int getPortStart() {
        return portStart;
    }

    public int getPortEnd() {
        return portEnd;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value.trim();
    }
}
