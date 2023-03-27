package searchengine.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.concurrent.Executor;

@Configuration
public class ConfigBeans {

    @Bean
    public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
        return new ForkJoinPoolFactoryBean();
    }

    @Bean
    public LemmaFinder lemmaFinderBean() throws IOException {
        return LemmaFinder.getInstance();
    }

    @Bean
    public ThreadPoolTaskExecutor threadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setDaemon(true);
        executor.setCorePoolSize(5);
        executor.setQueueCapacity(10);
        executor.initialize();
        return executor;
    }

    // делать это в другой ветке как фича когда все доделается, чтобы ускорить парсер и потоки не ждали дргу друга
    // TODO: в task можно не ждать join решение по задаче
    //  а только в главном классе
    //  поставить флаг на запщен ли compute в главном классе
    //  results по страницам класть в set но т.к. итоговый результать boolean это может быть флаг
    //  по каждому методу как прошел он кладет в результать true или false
    //  в случае ошибок можно класть false
    // не получися так сделать потому что тогда закончится поток по главному сайту,
    // можно сделать конутер на кол-во запущенных задач и кол-во полученных результатов, и когда он сравняется
    // значит задача законченна, но это должен быть бесконечный цикл который првоеряет этим переменные

    // и вообще PageIndexingTask не надо наследовать от stopCompute

    // TODO: перенсти в sercher regex паттерны в static

    // ManyToMany точно ли не нужна, приходится идти циклом по всем значениям и делать запрос
    // может с ManyTomany Hibernate быстрее работаетт обьединяя запросы джоином таблиц

    // TODO: проверить порядок в котором кладутся в Set snnippets

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
