package com.sawhub.hub.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Pipeline de IA da ata (M06) roda em `@Async` sobre um pool dedicado e pequeno — não é fila
 * de alto volume (uma mentoria de cada vez, upload é evento raro), mesmo raciocínio do
 * CLAUDE.md de não introduzir infra de fila pesada (RabbitMQ/SQS) sem necessidade comprovada. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ataProcessamentoExecutor")
    public Executor ataProcessamentoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ata-ia-");
        executor.initialize();
        return executor;
    }
}
