package com.quertimizer.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutorConfig {

    @Bean(name = "problemExecutingExecutor")
    public TaskExecutor problemExecutingExecutor() {
        // 문제 실행용 비동기 실행기 생성
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 문제 SQL 실행 전용 실행기
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("problem-executing-");
        executor.initialize();

        return executor;
    }

}
