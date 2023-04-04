package searchengine.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import searchengine.services.utils.LemmaFinder;

import java.io.IOException;

@Configuration
public class ConfigBeans {

    @Bean
    public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
        ForkJoinPoolFactoryBean pool = new ForkJoinPoolFactoryBean();
        return pool;
    }

    @Bean
    public LemmaFinder lemmaFinderBean() throws IOException {
        return LemmaFinder.getInstance();
    }

    @Bean
    public ThreadPoolTaskExecutor threadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setDaemon(true);
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }
}
