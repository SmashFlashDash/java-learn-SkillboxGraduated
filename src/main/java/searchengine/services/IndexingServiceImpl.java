package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.JsoupConfig;
import searchengine.config.LemmaFinder;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteData;
import searchengine.exceptions.OkError;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.indexing.AbstractIndexingTask;
import searchengine.services.indexing.PageIndexingTask;
import searchengine.services.indexing.SiteIndexingTask;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    // @PersistenceContext
    // private final EntityManager entityManager;
    // @Qualifier("threadExecutor2")
    // private final Executor threadPool;
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JsoupConfig jsoupConfig;
    private final ForkJoinPool forkJoinPool;
    @Qualifier("threadExecutor")
    private final ThreadPoolTaskExecutor executor;
    private final Set<AbstractIndexingTask> runningIndexingTasks = Collections.synchronizedSet(new HashSet<>());
    private final Map<AbstractIndexingTask, Future<Void>> mapTaskThread = Collections.synchronizedMap(new HashMap<>());
    private final LemmaFinder lf;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Override
    public boolean isIndexing() {
        return mapTaskThread.isEmpty();
    }

    @Override
    public IndexingResponse sitesIndexing() {
        List<Site> sitesList = sites.getSites();
        if (!runningIndexingTasks.isEmpty()) {
            throw new OkError("Индексация уже запущена");
        } else if (sitesList.isEmpty()) {
            throw new OkError("В конфигурации не указаны сайты для индексировния");
        }
        List<SiteData> sitesData = getSiteConfigs(sitesList);

        for (SiteData siteData : sitesData) {
            SiteEntity siteEntity = new SiteEntity(siteData.getName(), siteData.getUrl().toString(), EnumSiteStatus.INDEXING);
            SiteIndexingTask task = new SiteIndexingTask(siteData, siteEntity, jsoupConfig, this, lf);
            mapTaskThread.put(task, executor.submit(() -> threadSiteIndexing(task, siteEntity, siteData)));
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexingSites() {
        if (isIndexing()) {
            throw new OkError("Индексация не запущена");
        }
        runningIndexingTasks.forEach(AbstractIndexingTask::stopCompute);
        // чтобы не лочился runningIndexingTasks в потоках
        new ArrayList<>(runningIndexingTasks).forEach(task -> {
            Future<Void> future = mapTaskThread.get(task);
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse pageIndexing(String urlString) {
        if (!isIndexing()) {
            throw new OkError("Индексация запущена");
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new OkError("Некорректный url: ".concat(urlString));
        }
        Site site = isUrlInConfig(url);
        if (site == null) {
            throw new OkError("Сайт не задан в конфигурации");
        }

        SiteEntity tmpSite = siteRepository.findByName(site.getName());
        SiteEntity siteEntity = tmpSite != null ? tmpSite : new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXING);
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity != null) {
            deletePage(pageEntity);
        }
        PageIndexingTask task = new PageIndexingTask(url, siteEntity, jsoupConfig, this, lf);
        mapTaskThread.put(task, executor.submit(() -> threadPageIndexing(task, siteEntity)));
        return new IndexingResponse(true);
    }

    private Void threadPageIndexing(PageIndexingTask task, SiteEntity siteEntity) {
        runningIndexingTasks.add(task);
        try {
            Boolean res = forkJoinPool.invoke(task);
            if (res) {
                siteEntity.setStatus(EnumSiteStatus.INDEXED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(EnumSiteStatus.FAILED);
        }
        saveSite(siteEntity);
        runningIndexingTasks.remove(task);
        mapTaskThread.remove(task);
        return null;
    }

    @Transactional
    public Void threadSiteIndexing(SiteIndexingTask task, SiteEntity siteEntity, SiteData siteData) {
        runningIndexingTasks.add(task);
        try {
            siteRepository.deleteAllByName(siteData.getName());
            saveSite(siteEntity);
            Boolean res = forkJoinPool.invoke(task);
            if (res) {
                siteEntity.setStatus(EnumSiteStatus.INDEXED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(EnumSiteStatus.FAILED);
        }
        saveSite(siteEntity);
        runningIndexingTasks.remove(task);
        // TODO: по звершению уберет task из словаря потоков
        mapTaskThread.remove(task);
        return null;
    }

    public void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
            // TODO: вылетает ошибка но не падает при сейве определенных Entity
            e.printStackTrace();
        }
    }

    public void savePage(PageEntity page) {
        logger.info("Вставить в БД: ".concat(page.getPath()));
        pageRepository.save(page);
    }

    public boolean isPageExistByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    // TODO: здесь костыль
    @Transactional
    public void saveLemmasIndexes(PageEntity page, Map<String, Integer> lemmas) {
        // entityManager.refresh(page);
        PageEntity freshPage = pageRepository.findById(page.getId()).get();
//        PageEntity freshPage = pageRepository.getReferenceById(page.getId());
        List<IndexEntity> indexEntities = new ArrayList<>();
        List<LemmaEntity> lemmaEntities = lemmas.entrySet().stream().map(item -> {
            LemmaEntity lemma = lemmaRepository.findBySiteIdAndLemma(freshPage.getSite().getId(), item.getKey());
            if (lemma == null) {
                lemma = new LemmaEntity(freshPage.getSite(), item.getKey(), 1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            indexEntities.add(new IndexEntity(freshPage, lemma, item.getValue() * 1.0F));
            return lemma;
        }).collect(Collectors.toList());
        lemmaRepository.saveAll(lemmaEntities);
        indexRepository.saveAll(indexEntities);
    }

    // TODO: сделать триггером или попробовать обьеденить запрос в один
    @Transactional
    public void deletePage(PageEntity pageEntity) {
//        List<Long> lemmaIds = pageEntity.getLemmas().stream().map(LemmaEntity::getId).collect(Collectors.toList());
        List<Long> lemmaIds = pageEntity.getIndexes().stream().map(i -> i.getLemma().getId()).collect(Collectors.toList());
        pageRepository.delete(pageEntity);
        lemmaRepository.deleteOneFrequencyLemmaByIndexes(lemmaIds);
        lemmaRepository.updateBeforeDeleteIndexes(lemmaIds);
    }

    private List<SiteData> getSiteConfigs(List<Site> sitesList) {
        List<SiteData> sitesData = new ArrayList<>();
        for (Site site : sitesList) {
            SiteData siteData = new SiteData();
            siteData.setName(site.getName());
            siteData.setMillis(site.getMillis());
            try {
                URL url = new URL(site.getUrl());
                siteData.setUrl(url);
            } catch (MalformedURLException e) {
                throw new OkError("Некорректный url: ".concat(site.getUrl()));
            }
            sitesData.add(siteData);
        }
        return sitesData;
    }

    private Site isUrlInConfig(URL url) {
        String uriHost = url.getHost().startsWith("www.") ? url.getHost().substring(4) : url.getHost();
        List<Site> sitesList = sites.getSites();
        return sitesList.stream().filter(s -> s.getUrl().contains(uriHost)).findAny().orElse(null);
    }

}
