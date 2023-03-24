package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.JsoupConfig;
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
import searchengine.config.LemmaFinder;
import searchengine.services.indexing.PageIndexingTask;
import searchengine.services.indexing.SiteIndexingTask;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    //    @PersistenceContext
//    private final EntityManager entityManager;
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JsoupConfig jsoupConfig;
    private final ForkJoinPool forkJoinPool;
    //    @Qualifier("threadExecutor2")
    private final Executor threadPool;
    private final Set<AbstractIndexingTask> runningTasks = Collections.synchronizedSet(new HashSet<>());
    private final LemmaFinder lf;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    // TODO: indexingService можно внедриь в task Autowired сделав SCope prototype
    public IndexingResponse startSitesIndexing() {
        List<Site> sitesList = sites.getSites();
        if (!runningTasks.isEmpty()) {
            throw new OkError("Индексация уже запущена");
        } else if (sitesList.isEmpty()) {
            throw new OkError("В конфигурации не указаны сайты для индексировния");
        }
        List<SiteData> sitesData = getSiteConfigs(sitesList);

        List<Thread> threads = new ArrayList<>();
        for (SiteData siteData : sitesData) {
            SiteEntity siteEntity = new SiteEntity(siteData.getName(), siteData.getUrl().toString(), EnumSiteStatus.INDEXING);
            SiteIndexingTask task = new SiteIndexingTask(siteData, siteEntity, jsoupConfig, this, lf);
            threads.add(new Thread(() -> threadSiteIndexing(task, siteEntity, siteData)));
        }
        threads.forEach(Thread::start);
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexingSites() {
        if (runningTasks.isEmpty()) {
            throw new OkError("Индексация не запущена");
        }
        runningTasks.forEach(AbstractIndexingTask::stopCompute);
        runningTasks.forEach(AbstractIndexingTask::join);
        return new IndexingResponse(true);
    }

    //TODO: добавить в forkJoin создать PageIndexingTask
    // сделать интерфейс с нужными полями и кидать их в один runnigTask
    // и запускать одной и той же птоковой функцией
    @Override
    public IndexingResponse pageIndexing(String urlString) {
        if (!runningTasks.isEmpty()) {
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
        // стереть page и indexes и update lemmas
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity != null) {
            deletePage(pageEntity);
        }

        PageIndexingTask task = new PageIndexingTask(url, siteEntity, jsoupConfig, this, lf);
        Thread thread = new Thread(() -> threadPageUpdate(task, siteEntity));
        thread.start();
        return new IndexingResponse(true);
    }

    private void threadPageUpdate(PageIndexingTask task, SiteEntity siteEntity) {
        runningTasks.add(task);
        try {
            Boolean res = forkJoinPool.invoke(task);
            if (res) {
                siteEntity.setStatus(EnumSiteStatus.INDEXED);
            }
        } catch (Exception e) {
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(EnumSiteStatus.FAILED);
        }
        saveSite(siteEntity);
        runningTasks.remove(task);
    }

    private void threadSiteIndexing(SiteIndexingTask task, SiteEntity siteEntity, SiteData siteData) {
        runningTasks.add(task);
        siteRepository.deleteByName(siteData.getName());
        saveSite(siteEntity);
        try {
            Boolean res = forkJoinPool.invoke(task);
            if (res) {
                siteEntity.setStatus(EnumSiteStatus.INDEXED);
            }
        } catch (Exception e) {
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(EnumSiteStatus.FAILED);
        }
        saveSite(siteEntity);
        runningTasks.remove(task);
    }

    @Transactional
    public void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
            // TODO: вылетает ошибка но не падает при сейве определенных Entity
            e.printStackTrace();
        }
    }

    @Transactional
    public void savePage(PageEntity page) {
        logger.info("Вставить в БД: ".concat(page.getPath()));
        pageRepository.save(page);
    }

    public boolean isPageExistByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    // TODO: здесь костыль надо обновить page или запросить
    @Transactional
    public void saveLemmasIndexes(PageEntity page, Map<String, Integer> lemmas) {
        // entityManager.refresh(page);     // нужен @Transactional на метод, не работает с методами в SiteIndexingTask
        PageEntity freshPage = pageRepository.findById(page.getId()).get();

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

    // TODO: не стирает page
    //  Cannot delete or update a parent row: a foreign key constraint fails (`search_engine`.`index`, CONSTRAINT `FK3uxy5s82mxfodai0iafb232cs` FOREIGN KEY (`page_id`) REFERENCES `page` (`id`))
    @Transactional
    public void deletePage(PageEntity pageEntity) {
        // TODO: можно сделать через триггер в БД или EntityListener на IndexEntity @PreRemove
        //  и заинжектить туда indexRepository, создавать бином через config?
        // TODO: получить все id и где frequncy = 1 удалить у остальных - 1
        //  через nativeQuery в LemmaEntity туда передать lemmas от page

        // List<Long> lemmaIds = pageEntity.getLemmas().stream().map(LemmaEntity::getId).collect(Collectors.toList());
        List<Long> lemmaIds = pageEntity.getIndexes().stream().map(i -> i.getLemma().getId()).collect(Collectors.toList());
        lemmaRepository.deleteOneFrequencyLemmaByIndexes(lemmaIds);
        lemmaRepository.updateBeforeDeleteIndexes(lemmaIds);

        // TODO: можно рефрешем воспользоваться
        pageEntity = pageRepository.findById(pageEntity.getId()).get();
        pageRepository.delete(pageEntity);
//        pageRepository.findById(pageEntity.getId()).ifPresent(pageRepository::delete);

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
