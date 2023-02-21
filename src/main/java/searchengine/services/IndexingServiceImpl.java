package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.EnumSiteStatus;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional
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
        if (sitesList.isEmpty()){
            return;
        }
        //удалять все имеющиеся данные по этому сайту (записи из таблиц site и page);
        deleteDataBySites(sitesList.stream().map(Site::getName).collect(Collectors.toList()));
        //создавать в таблице site новую запись со статусом INDEXING; б
        for (Site site : sitesList){
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setUrl(site.getUrl());
            siteEntity.setName(site.getName());
            siteEntity.setStatus(EnumSiteStatus.INDEXING);
//            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);
        }
    }

    @Override
    public void stopIndexingSites() {

    }

    /**
     * получить id сайта по названию в таблице Sites и по этому id стереть Page
     * если свзяать поля Cascade то можно удалить только по id в site
     */
    @Transactional
    protected void deleteDataBySites(List<String> siteNames){
        siteRepository.deleteAllByNameIn(siteNames);
    }
}
