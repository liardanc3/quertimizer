package com.quertimizer.judge.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.entity.DatabaseNodeConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DatabaseNodeConfigOutput {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private final boolean enabled;
    private final int maxConcurrency;
    private final LocalDateTime updatedAt;

    public static DatabaseNodeConfigOutput from(DatabaseNodeConfig databaseNodeConfig) {
        // 도메인 설정을 출력 모델로 변환
        return new DatabaseNodeConfigOutput(
                databaseNodeConfig.getDatabaseId(), databaseNodeConfig.getDatabaseName(), databaseNodeConfig.getDbmsType(),
                databaseNodeConfig.isEnabled(), databaseNodeConfig.getMaxConcurrency(), databaseNodeConfig.getUpdatedAt()
        );
    }
}
