package com.quertimizer.monitoring.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.monitoring.domain.entity.JudgeConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JudgeConfigOutput {

    private final String databaseId;
    private final String databaseName;
    private final DbmsType dbmsType;
    private final boolean enabled;
    private final int maxConcurrency;
    private final LocalDateTime updatedAt;

    public static JudgeConfigOutput from(JudgeConfig judgeConfig) {
        // 도메인 설정을 출력 모델로 변환
        return new JudgeConfigOutput(
                judgeConfig.getDatabaseId(), judgeConfig.getDatabaseName(), judgeConfig.getDbmsType(),
                judgeConfig.isEnabled(), judgeConfig.getMaxConcurrency(), judgeConfig.getUpdatedAt()
        );
    }
}
