package com.quertimizer.judge.config;

import com.quertimizer.judge.application.port.out.LvmSnapshotConfigRepositoryPort;
import com.quertimizer.judge.domain.entity.LvmSnapshotConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@Profile("test")
public class JudgeInfrastructureTestConfig {

    @Bean
    @Primary
    public LvmSnapshotConfigRepositoryPort testLvmSnapshotConfigRepositoryPort() {
        return () -> Optional.of(LvmSnapshotConfig.restore(
                "default", "/tmp", "test_vg", "test_pool", "test_base", 60, 1, LocalDateTime.now()
        ));
    }
}
