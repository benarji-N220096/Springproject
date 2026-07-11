package com.employeeportal.securityapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core pool size is the minimum number of worker threads to keep alive
        executor.setCorePoolSize(2);
        // Maximum pool size is the maximum number of threads that can ever be created
        executor.setMaxPoolSize(5);
        // Queue capacity defines how many tasks can wait in the queue if all core threads are busy
        executor.setQueueCapacity(50);
        // Thread name prefix helps easily identify async threads in logs
        executor.setThreadNamePrefix("AsyncWorkerThread-");
        executor.initialize();
        return executor;
    }
}
