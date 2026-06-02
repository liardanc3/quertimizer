package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.model.DatabaseCluster;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.DatabaseNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseNodeService {

    private final Options options;

    public DatabaseNode requireNode(String databaseId) {
        // DB 노드 ID 기준 LVM DB 노드 필수 조회
        return options.requireNode(databaseId);
    }

    public int startupTimeoutSeconds() {
        // 런타임 DB 프로세스 시작 제한 시간 반환
        return options.getStartupTimeoutSeconds();
    }
}
