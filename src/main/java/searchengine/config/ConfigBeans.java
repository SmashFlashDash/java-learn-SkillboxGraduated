package searchengine.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

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

    // TODO: вставить сюда execitoreThread
    //  - можно сделать бин котоырй делает set в static поля PAge и UpdateTask
    //  - чтобы не передвать их констркутором

    // сделать чтобы брал релевантность в тэг
    // подготовить коммит с вопросами

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
