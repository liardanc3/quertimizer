package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.DatabaseCluster;
import com.quertimizer.judge.application.model.DatabaseLease;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DatabaseNodeService {

    private final DatabaseCluster databaseCluster;
    private final Options options;

    public DatabaseLease acquireDatabase(DbmsType dbmsType) {
        // DBMS 기준 사용 가능한 DB 노드 점유
        return databaseCluster.acquire(dbmsType);
    }

    public DatabaseLease acquireDatabaseNode(String nodeId) {
        // DB 노드 노드 ID 기준 DB 노드 점유
        return databaseCluster.acquireNode(nodeId);
    }

    public List<Database> getConfiguredDatabases() {
        // 설정된 DB 노드 목록 반환
        return databaseCluster.getConfiguredDatabases();
    }

    public Optional<DatabaseNode> findNode(String databaseId) {
        // DB 노드 ID 기준 LVM DB 노드 조회
        return options.findNode(databaseId);
    }

    public DatabaseNode requireNode(String databaseId) {
        // DB 노드 ID 기준 LVM DB 노드 필수 조회
        return options.requireNode(databaseId);
    }

    public String baseTemplateVersion() {
        // 기준 템플릿 버전 반환
        return options.getBaseTemplateVersion();
    }

    public int startupTimeoutSeconds() {
        // 런타임 DB 프로세스 시작 제한 시간 반환
        return options.getStartupTimeoutSeconds();
    }
}
