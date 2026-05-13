package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.model.ProvisionedEnvironment;
import com.quertimizer.judge.application.model.SqlExecutorTicket;
import com.quertimizer.judge.application.port.out.EnvironmentProvisioner;
import com.quertimizer.judge.application.port.out.SqlExecutionPort;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.quertimizer.judge.domain.model.JudgeFailReason.DROPPED_ENVIRONMENT;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_ENVIRONMENT_ID;

@Service
@RequiredArgsConstructor
public class SqlExecutorPool {

    private final EnvironmentProvisioner environmentProvisioner;
    private final SqlExecutionPort sqlExecutionPort;
    private final DatasetTemplateService datasetTemplateService;
    private final ConcurrentHashMap<JudgeEnvironmentId, PersistentSqlExecutorEnvironment> environments = new ConcurrentHashMap<>();

    public SqlExecutorTicket requestExecutor(DbmsType dbmsType, QueuePriority priority,
                                             QueueStatusListener statusListener) {
        // SQL 실행기 대기열 ticket 발급
        return new SqlExecutorTicket(dbmsType, priority, statusListener);
    }

    public SqlExecutorTicket requestExecutor(JudgeEnvironmentId environmentId, QueuePriority priority,
                                             QueueStatusListener statusListener) {
        // 기존 실행 환경의 DBMS 기준 SQL 실행기 ticket 발급
        return requestExecutor(
                requireEnvironment(environmentId).environment.getExecutionEnvironment().getDatabase().getDbmsType(),
                priority, statusListener
        );
    }

    public SqlExecutor acquireExecutor(SqlExecutorTicket ticket) {
        // ticket에 연결된 SQL 실행기 생성
        return new SqlExecutor(ticket, environments, environmentProvisioner, sqlExecutionPort, datasetTemplateService);
    }

    public boolean hasEnvironment(JudgeEnvironmentId environmentId) {
        // 영속 실행 환경 등록 여부 확인
        return environments.containsKey(environmentId);
    }

    public void dropEnvironment(JudgeEnvironmentId environmentId) {
        // 제거 대상 실행 환경 조회
        PersistentSqlExecutorEnvironment persistentEnvironment = environments.get(environmentId);
        if (persistentEnvironment == null) {
            return;
        }

        // 실행 환경 단위 lock 확보 후 영속 실행 환경 제거
        persistentEnvironment.lock.lock();
        try {
            if (persistentEnvironment.dropped) {
                return;
            }

            persistentEnvironment.dropped = true;
            environmentProvisioner.drop(persistentEnvironment.environment);
            environments.remove(environmentId, persistentEnvironment);
        } catch (RuntimeException exception) {
            persistentEnvironment.dropped = false;
            throw exception;
        } finally {
            persistentEnvironment.lock.unlock();
        }
    }

    public void dropDataset(DatasetTemplateDefinition templateDefinition) {
        // 데이터셋 템플릿 제거 위임
        datasetTemplateService.dropDatasetTemplate(templateDefinition);
    }

    public boolean hasActiveExecution(JudgeExecutionId executionId) {
        // 실행 ID 기준 취소 가능한 SQL 실행 존재 여부 확인
        return sqlExecutionPort.hasActiveExecution(executionId);
    }

    public void cancel(JudgeExecutionId executionId) {
        // 실행 ID 기준 SQL 실행 취소 요청
        sqlExecutionPort.cancel(executionId);
    }

    private PersistentSqlExecutorEnvironment requireEnvironment(JudgeEnvironmentId environmentId) {
        // 영속 실행 환경 조회
        PersistentSqlExecutorEnvironment environment = environments.get(environmentId);
        if (environment == null) {
            throw new IllegalArgumentException(UNKNOWN_ENVIRONMENT_ID.format(environmentId));
        }

        return environment;
    }

    static final class PersistentSqlExecutorEnvironment {
        final ProvisionedEnvironment environment;
        final ReentrantLock lock = new ReentrantLock();
        private boolean dropped;

        PersistentSqlExecutorEnvironment(ProvisionedEnvironment environment) {
            this.environment = environment;
        }

        void requireAvailable() {
            // 제거된 실행 환경 사용 차단
            if (dropped) {
                throw new IllegalStateException(DROPPED_ENVIRONMENT.format(environment.getExecutionEnvironment().getEnvironmentId()));
            }
        }
    }
}
