package searchengine.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

    // TODO: SnippetParser бывает с предыдущего сниппета суфиикс пост заезжает на следующий сниппет
    // в SnippetParser.getSnippet если получить несколько Snippet то постфикс прудыдщего может наложиться и будет
    // повторение, поэтому можно брать первый сниппет наибольший по длинне, поменять сортировку Set<Snippet>

    // TODO: ManyToMany точно ли не нужна, приходится идти циклом по всем значениям и делать запрос
    //  может с ManyTomany Hibernate быстрее работаетт обьединяя запросы джоином таблиц

    // TODO:
    //  SearchService
    //  - не получается получить правильно сниппеты соответсвующие контенту сайта
    //  - отсортировать и вернуть список сайтов
    //  - как возвращать Pageble<SearchData>
    //      public Page<Book> getPageOfNewBooksDateBetween(Date from, Date to, Integer offset, Integer limit) {
    //          return bookRepository.findAllByPubDateBetweenOrderByPubDateDesc(from, to, PageRequest.of(offset, limit));
    //      }
    //  - searchDataList можно идти forkJoin хотя и так быстро ищет, поделив задачу по кол-ву страниц
    //  IndexingService
    //  - можно ли SiteIndexingTask внедрить service, jsoupConfig, lf через Spring
    //  - костыль в indexesService.saveLemmasIndexes не работает Transactional в ForkJoinPool
    //      надо entityManager.refresh(page) ужен @Transactional на метод, не работает с методами в SiteIndexingTask
    //      entityManager.refresh(page)
    //      TransactionSynchronizationManager.setActualTransactionActive(true);
    //      TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
    //      TransactionSynchronizationManager.initSynchronization();
    //  - там же startSitesIndexing можно сделать через threadExecutor

}
