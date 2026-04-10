package com.quertimizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutorConfig {

    @Bean(name = "sessionManagingExecutor")
    public TaskExecutor sessionManagingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 세션 메모리/DB 동기화 작업 전용 실행기
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("session-managing-");
        executor.initialize();

        return executor;
    }

    @Bean(name = "problemExecutingExecutor")
    public TaskExecutor problemExecutingExecutor() {
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
