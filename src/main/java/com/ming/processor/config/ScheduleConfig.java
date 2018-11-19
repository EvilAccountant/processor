package com.ming.processor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

@Configuration
@EnableAsync
//定时任务调用一个线程池中的线程。
public class ScheduleConfig implements SchedulingConfigurer, AsyncConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        //参数传入一个size为10的线程池
        scheduledTaskRegistrar.setScheduler(Executors.newScheduledThreadPool(10));
    }

//    @Bean("asyncTread")
//    public TaskScheduler asyncTread() {
//        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//        scheduler.setThreadNamePrefix("asyncTread---");
//        scheduler.setPoolSize(10);
//        scheduler.initialize();
//        return scheduler;
//    }

//    @Bean("asyncTread")
//    public ThreadPoolTaskExecutor asyncTread() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(10);
//        executor.setMaxPoolSize(20);
//        executor.setQueueCapacity(200);
//        executor.setKeepAliveSeconds(60);
//        executor.setThreadNamePrefix("asyncTreadPool---");
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        return executor;
//    }
}