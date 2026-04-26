package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ExecutionDatabasePool {

    private final Map<DbmsType, List<ExecutionDatabaseWorker>> workersByDbms = new EnumMap<>(DbmsType.class);

    public ExecutionDatabasePool(JudgeDatabaseProperties judgeDatabaseProperties) {
        workersByDbms.put(DbmsType.POSTGRESQL, judgeDatabaseProperties.getExecutionDatabases(DbmsType.POSTGRESQL).stream()
                .map(properties -> new ExecutionDatabaseWorker(new ExecutionDatabaseConnectionInfo(
                        DbmsType.POSTGRESQL,
                        properties.getName(),
                        properties.getUrl(),
                        properties.getUsername(),
                        properties.getPassword()
                )))
                .toList());
        workersByDbms.put(DbmsType.ORACLE, judgeDatabaseProperties.getExecutionDatabases(DbmsType.ORACLE).stream()
                .map(properties -> new ExecutionDatabaseWorker(new ExecutionDatabaseConnectionInfo(
                        DbmsType.ORACLE,
                        properties.getName(),
                        properties.getUrl(),
                        properties.getUsername(),
                        properties.getPassword()
                )))
                .toList());
    }

    public List<ExecutionDatabaseWorker> getWorkers(DbmsType dbmsType) {
        // DBMS 유형별 execution worker 목록을 조회
        return workersByDbms.getOrDefault(dbmsType, List.of());
    }

    @Getter
    public static final class ExecutionDatabaseWorker {
        private final ExecutionDatabaseConnectionInfo connectionInfo;
        private boolean busy;

        private ExecutionDatabaseWorker(ExecutionDatabaseConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
        }

        void markBusy() {
            busy = true;
        }

        void release() {
            busy = false;
        }
    }
}
