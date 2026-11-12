package com.techdoc.translator.translation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TranslationConfig {

    /**
     * 번역 작업 실행기. 기본은 비동기(1개 스레드, 문서를 순서대로 처리).
     * app.translation.async=false 이면 호출한 스레드에서 바로 실행한다(테스트용).
     */
    @Bean
    public TaskExecutor translationExecutor(@Value("${app.translation.async:true}") boolean async) {
        if (!async) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("translate-");
        executor.initialize();
        return executor;
    }
}
