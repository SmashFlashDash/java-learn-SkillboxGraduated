package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.classes.SiteIndexingTask;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.EnumSiteStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.beans.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final JsoupConfig jsoupConfig;

    // TODO: в RecusiveAction нужно иметь доступ к сервайсу indexing
    //  сервайс должен уметь сохранять страницы в таблицу и полчуать
    //  можно сделать через интерфейс indexingService или передать repository чтобы меньше зависимость
    //  можно попробовать реализовать через Spring зависимостью
//    https://stackoverflow.com/questions/31757216/spring-cannot-propagate-transaction-to-forkjoins-recursiveaction
//    @Configuration
//    public class PartitionersConfig {
//
//        @Bean
//        public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean() {
//            final ForkJoinPoolFactoryBean poolFactory = new ForkJoinPoolFactoryBean();
//            return poolFactory;
//        }
//    }
//    @Service
//    @Transactional
//    public class MyService {
//
//        @Autowired
//        private OtherService otherService;
//
//        @Autowired
//        private ForkJoinPool forkJoinPool;
//
//        @Autowired
//        private MyDao myDao;
//
//        public void performPartitionedActionOnIds() {
//            final ArrayList<UUID> ids = otherService.getIds();
//
//            MyIdPartitioner task = new MyIdsPartitioner(ids, myDao, 0, ids.size() - 1);
//            forkJoinPool.invoke(task);
//        }
//    }

//    @Transactional
    @Override
    public void startIndexingSites() {
        // TODO: надо индексировать сайты потоком на каждый сайт ForkJoinPool
//        В сервисе индексации сайтов пропишите код, который будет брать из
//        конфигурации приложения список сайтов и по каждому сайту:
//удалять все имеющиеся данные по этому сайту (записи из таблиц site и page);
//создавать в таблице site новую запись со статусом INDEXING;
//обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
//в процессе обхода постоянно обновлять дату и время в поле status_time таблицы site на текущее;
//по завершении обхода изменять статус (поле status) на INDEXED;
//если произошла ошибка и обход завершить не удалось, изменять статус на FAILED и вносить в поле last_error понятную информацию о произошедшей ошибке.
//Для перехода по очередной ссылке должен создаваться новый поток при
//        помощи Fork-Join. Этот поток будет получать содержимое страницы и
//        перечень ссылок, которые есть на этой странице (значений атрибутов
//        href HTML-тегов <a>), при помощи JSOUP

        // можно сделать потокобезопасный метод, который делает записи в БД в сервисе
        // можно сделать очередь, класть туда запросы,
//        List<Site> sitesList = sites.getSites();
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        for (Site s : sitesList){
//            forkJoinPool.invoke(new SiteIndexingTask(s));
//        }


        // TODO: надо написать репозиторий для page и site и подключить в сервис
        List<Site> sitesList = sites.getSites();
        if (sitesList.isEmpty()) {
            return;
        }
        // TODO: вернуть кол-во удаленных записей @Modified
        deleteDataBySites(sitesList.stream().map(Site::getName).collect(Collectors.toList()));
        // TODO: можно использвать spring класс фабрику с настроенным ForkJoinBean
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        List<Callable<String>> sitesThreads = new ArrayList<>();
//        List<Future<String>> sitesThreads = new ArrayList<>();
        for (Site site : sitesList) {
            saveSite(site, EnumSiteStatus.INDEXING);
            // кидаем в fork JoinPool сайт// он фигачит поиск и пишет в таблицу page links
            // лучше передать сервис с методами @Transactional чтобы потоко безопасно гонять по данным
            // TODO: можно передать класс dispatcher на разные сервисы, должен быть компонентом
            //  если нужен только один сервис то один сервис
            // содаем таск, сэйвим в список таск, запускаем в ForkJoin
            SiteIndexingTask task = new SiteIndexingTask(site.getUrl(), jsoupConfig, this);
            sitesThreads.add(task);
//            forkJoinPool.invoke(task);
            forkJoinPool.execute(task);     // начать выплднять задачу и не ждать join
//            new SiteIndexingTask(site.getUrl(), jsoupConfig)
//            forkJoinPool.invoke(new SiteIndexingTask(site.getUrl(), jsoupConfig));
//            forkJoinPool.execute(new SiteIndexingTask(site.getUrl(), jsoupConfig));

//            new SiteIndexingTask(site.getUrl(), jsoupConfig).fork();
//            sitesThreads.add(forkJoinPool.invoke(new SiteIndexingTask(site.getUrl(), jsoupConfig)));
        }
        // TODO: стартуем futereTasks invokeAll, что будет ждать пока все выполняться
        // то

//        List<Future<String>> s = forkJoinPool.invokeAll(sitesThreads);
//        List<Future<String>> s2 = forkJoinPool.;
//        for (Future<String> ss : s) {
//            ss.isDone();
//        }
    }

    @Override
    public void stopIndexingSites() {

    }

    /**
     * получить id сайта по названию в таблице Sites и по этому id стереть Page
     * если свзяать поля Cascade то можно удалить только по id в site
     */
    @Override
    @Transactional
    public void deleteDataBySites(List<String> siteNames){
        siteRepository.deleteAllByNameIn(siteNames);
    }

    @Override
    @Transactional
    public void saveSite(Site site, EnumSiteStatus status) {
        SiteEntity siteEntity = new SiteEntity();
        siteEntity.setUrl(site.getUrl());
        siteEntity.setName(site.getName());
        siteEntity.setStatus(status);
        // siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }
}
