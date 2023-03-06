package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.classes.SiteIndexingTaskInvoke;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.EnumSiteStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImplInvoke implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final JsoupConfig jsoupConfig;
    private final ForkJoinPool forkJoinPool;
    private final Set<SiteIndexingTaskInvoke> indexingTasks = Collections.synchronizedSet(new HashSet<>());
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Override
    public IndexingResponse startIndexingSites() {
        List<Site> sitesList = sites.getSites();
        if (!indexingTasks.isEmpty()) {
            logger.error(String.format("Индексация уже запущена: %s", indexingTasks));
            return new IndexingResponse(false, "Индексация уже запущена");
        } else if (sitesList.isEmpty()) {
            logger.error(String.format("В конфиге не указаны сайты: %s", indexingTasks));
            return new IndexingResponse(false, "В конфигурации не указаны сайты для индексировния");
        }

        List<Thread> threads = new ArrayList<>();
        for (Site site : sitesList) {
            SiteEntity siteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXING);
            SiteIndexingTaskInvoke task;
            try {
                URL uri = new URL(site.getUrl());
                task = new SiteIndexingTaskInvoke(uri, siteEntity, site.getMillis(), jsoupConfig, this);
            } catch (MalformedURLException e) {
                return new IndexingResponse(false, "В конфигурации некорректный url: ".concat(site.getUrl()));
            }
            //siteRepository.deleteByName(site.getName());
            //saveSite(siteEntity);
            indexingTasks.add(task);

            threads.add(new Thread(() -> {
                siteRepository.deleteByName(site.getName());
                saveSite(siteEntity);
                Boolean res;
                try {
                    res = forkJoinPool.invoke(task);
                } catch (Exception e) {
                    res = false;
                    siteEntity.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()));
                    siteEntity.setStatus(EnumSiteStatus.FAILED);
                    saveSite(siteEntity);
                }
                indexingTasks.remove(task);
                if (res) {
                    siteEntity.setStatus(EnumSiteStatus.INDEXED);
                    saveSite(siteEntity);
                }
            }));
        }
        threads.forEach(Thread::start);
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexingSites() {
        if (indexingTasks.isEmpty()) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        indexingTasks.forEach(SiteIndexingTaskInvoke::stopCompute);
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse pageIndexing(String url) {
        // TODO: должен ли рабоать когда запущена индексация
        //  вернуть ошибку если домен сайта не из Jsoup конфига
        //  можно ли в Jsoup config сразу сделать URI из String
        // должен ли SiteIndexingResponse включать функционал PageIndexing
        //  default значение в Entity таблицы lemma
        // в таблицу lemma добавляются связки lemma страница
        if (!indexingTasks.isEmpty()) {
            return new IndexingResponse(false, "Индексация запущена");
        }

        try {
            URL uri = new URL(url);
            SiteEntity ss = new SiteEntity();
            for (Site s : sites.getSites()) {
                if (s.getUrl().contains(uri.getHost())) {
                    siteRepository.findAllByNameIn(Arrays.asList(s.getName()));
                    siteRepository.findByName(s.getName());

                    ss.setName(s.getName());
                    ss.setUrl(s.getUrl());
                    break;
                }
            }
        } catch (MalformedURLException e) {
            return new IndexingResponse(false, "Некорректный url: ".concat(url));
        }


        return new IndexingResponse(false);
    }

    //    @Override
//    @Transactional
//    public void deleteDataBySites(List<String> siteNames) {
//        siteRepository.deleteAllByNameIn(siteNames);
//    }

    // TODO: вылетает ошибка но не падает при сейве определенных Entity
    @Override
    public synchronized void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    // TODO: можно сделать batch insert
    //  складывать все и flush как закончится парсинг
    //  но тогда проверять page в таблице надо еще и в batch
    @Override
    public synchronized void savePage(PageEntity page) {
        // siteEntity.setStatusTime(LocalDateTime.now());
        logger.info("Вставить в БД: ".concat(page.getPath()));
        pageRepository.save(page);
    }

    @Override
    public boolean isPageExistByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    @Override
    public Integer countPages() {
        return pageRepository.findAll().size();
    }
}
