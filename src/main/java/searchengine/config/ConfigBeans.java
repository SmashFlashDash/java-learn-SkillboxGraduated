package searchengine.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import searchengine.services.indexing.LemmaFinder;

import java.io.IOException;

@Configuration
public class ConfigBeans {

    @Bean
    public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
        final ForkJoinPoolFactoryBean poolFactory = new ForkJoinPoolFactoryBean();
        return poolFactory;
    }

    @Bean
    public LemmaFinder lemmaFinderBean() throws IOException {
        return LemmaFinder.getInstance();
    }
}
