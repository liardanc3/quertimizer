package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

@Data
public class DatabaseNode {

    private final String databaseId;
    private final String containerName;
    private final String host;
    private final int portStart;
    private final int portEnd;
    private final String databaseName;
    private final String rootPassword;

    public DatabaseNode(String databaseId, String containerName, String host,
                       int portStart, int portEnd, String databaseName, String rootPassword) {
        this.databaseId = databaseId.trim();
        this.containerName = containerName.trim();
        this.host = host.trim();
        this.portStart = portStart;
        this.portEnd = portEnd;
        this.databaseName = databaseName != null ? databaseName.trim() : "";
        this.rootPassword = rootPassword != null ? rootPassword : "";
    }

    public String createJdbcUrl(DbmsType dbmsType, int port) {
        // DBMS별 DB process JDBC URL 생성
        return switch (dbmsType) {
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                    + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        };
    }

    public String resolveRootPassword(String fallbackPassword) {
        // node 전용 root password 우선 적용
        return !rootPassword.isBlank() ? rootPassword : fallbackPassword;
    }
}
