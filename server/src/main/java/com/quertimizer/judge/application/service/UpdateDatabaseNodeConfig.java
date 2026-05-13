package com.quertimizer.judge.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.log.Log;
import com.quertimizer.judge.application.input.DatabaseNodeConfigUpdateInput;
import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;
import com.quertimizer.judge.application.port.in.UpdateDatabaseNodeConfigUseCase;
import com.quertimizer.judge.application.port.out.DatabaseNodeConfigRepositoryPort;
import com.quertimizer.judge.application.port.out.DatabaseSnapshotPort;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import com.quertimizer.judge.domain.model.DatabaseNodeFailReason;
import com.quertimizer.judge.domain.model.DatabaseNodeSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateDatabaseNodeConfig implements UpdateDatabaseNodeConfigUseCase {

    private final DatabaseNodeConfigRepositoryPort databaseNodeConfigRepositoryPort;
    private final DatabaseSnapshotPort databaseSnapshotPort;

    /**
     * DB 실행 환경 동적 설정을 저장한다.
     *
     * <ol>
     *   <li>설정 대상 DB 노드 조회
     *   <li>동시 실행 수 검증
     *   <li>기존 설정 변경 또는 신규 설정 저장
     * </ol>
     *
     * @param input 변경할 database와 동시 실행 설정
     */
    @Override
    @Transactional
    @Log("DB 노드 설정 수정")
    public DatabaseNodeConfigOutput execute(DatabaseNodeConfigUpdateInput input) {
        DatabaseNodeSnapshot node = databaseSnapshotPort.createSnapshot().getNodes().stream()
                .filter(candidate -> candidate.getDatabaseId().equals(input.getDatabaseId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        DatabaseNodeFailReason.DATABASE_NODE_CONFIG_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND
                ));

        if (input.getMaxConcurrency() <= 0 || input.getMaxConcurrency() > node.getTotalPortCount()) {
            throw new BusinessException(DatabaseNodeFailReason.INVALID_MAX_CONCURRENCY.getMessage(), HttpStatus.BAD_REQUEST);
        }

        DatabaseNodeConfig databaseNodeConfig = databaseNodeConfigRepositoryPort.findByDatabaseId(input.getDatabaseId())
                .map(config -> config.update(input.isEnabled(), input.getMaxConcurrency()))
                .orElseThrow(() -> new BusinessException(
                        DatabaseNodeFailReason.DATABASE_NODE_CONFIG_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND
                ));
        return DatabaseNodeConfigOutput.from(databaseNodeConfigRepositoryPort.save(databaseNodeConfig));
    }
}
