package com.quertimizer.global.config;

import com.quertimizer.global.log.LogMdcContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncExecutorConfig {

    @Bean
    public TaskDecorator logMdcTaskDecorator() {
        // 비동기 작업 실행 시 요청 스레드의 로그 주체 전파
        return LogMdcContext::wrap;
    }

    @Bean
    public ThreadPoolTaskExecutor problemCreateTaskExecutor(TaskDecorator logMdcTaskDecorator) {
        // 문제 생성 장기 작업 전용 executor 구성
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("problem-create-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setTaskDecorator(logMdcTaskDecorator);
        executor.initialize();
        return executor;
    }

    @Bean
    public ThreadPoolTaskExecutor problemSubmitTaskExecutor(TaskDecorator logMdcTaskDecorator) {
        // 문제 제출 채점 병렬 작업 전용 executor 구성
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("problem-submit-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(40);
        executor.setTaskDecorator(logMdcTaskDecorator);
        executor.initialize();
        return executor;
    }
}
