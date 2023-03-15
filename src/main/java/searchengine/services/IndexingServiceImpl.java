package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.indexingTasks.AbstractIndexingTask;
import searchengine.dto.indexingTasks.LemmaFinder;
import searchengine.dto.indexingTasks.PageIndexingTask;
import searchengine.dto.indexingTasks.SiteIndexingTask;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteConfig;
import searchengine.exceptions.OkError;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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
        List<SiteConfig> siteConfigs = getSiteConfigs(sitesList);

        List<Thread> threads = new ArrayList<>();
        for (SiteConfig siteConfig : siteConfigs) {
            SiteEntity siteEntity = new SiteEntity(siteConfig.getName(), siteConfig.getUrl().toString(), EnumSiteStatus.INDEXING);
            SiteIndexingTask task = new SiteIndexingTask(siteConfig, siteEntity, jsoupConfig, this, lf);
            threads.add(new Thread(() -> {
                runningTasks.add(task);
                // TODO: почему в модели на ManyToMany без cascade стираются indexes
                siteRepository.deleteByName(siteConfig.getName());
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
            }));
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
        SiteEntity tmpSiteEntity = siteRepository.findByName(site.getName());
        if (tmpSiteEntity == null) {
            tmpSiteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXED);
        }
        SiteEntity siteEntity = tmpSiteEntity;

        // TODO: можно вынести в метод transactional
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity != null) {
            // TODO: можно сделать через триггер в БД или EntityListener на IndexEntity @PreRemove
            //  и заинжектить туда indexRepository, создавать бином через config?

            // TODO: при стирании page lemma уменьшает frequency на 1 если frequency -1 > 0
//             List<LemmaEntity> lemmasToDelte = new ArrayList<>();
//             List<LemmaEntity> lemmasToUpdate = pageEntity.getLemmas().stream().filter(lemma -> {
//                 int newFrequency = lemma.getFrequency() - 1;
//                 if (newFrequency > 0) {
//                     lemma.setFrequency(newFrequency);
//                     return true;
//                 } else {
//                     lemmasToDelte.add(lemma);
//                     return false;
//                 }
//             }).collect(Collectors.toList());
//             pageRepository.delete(pageEntity);
//             lemmaRepository.deleteAll(lemmasToDelte);
//             lemmaRepository.saveAll(lemmasToUpdate);

            // TODO: получить все id и где frequncy = 1 удалить у остальных - 1
            //  через nativeQuery в LemmaEntity туда передать lemmas от page
//            List<Long> lemmaIds = pageEntity.getLemmas().stream().map(LemmaEntity::getId).collect(Collectors.toList());
            List<Long> lemmaIds = pageEntity.getIndexes().stream().map(i -> i.getLemma().getId()).collect(Collectors.toList());
            lemmaRepository.deleteOneFrequencyLemmaByIndexes(lemmaIds);
            lemmaRepository.updateBeforeDeleteIndexes(lemmaIds);
            pageEntity = pageRepository.findById(pageEntity.getId()).get();
            pageRepository.delete(pageEntity);
        }

        PageIndexingTask task = new PageIndexingTask(url, siteEntity, jsoupConfig, this, lf);
        Thread thread = new Thread(() -> {
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
        });
        thread.start();
        return new IndexingResponse(true);
    }

    public Integer countPages() {
        return pageRepository.findAll().size();
    }

    // TODO: вылетает ошибка но не падает при сейве определенных Entity
    @Transactional
    public void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
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

    @Transactional
    public void saveLemmasMap(PageEntity page, Map<String, Integer> lemmas) {
        // TODO: здесь костыль
        // entityManager.refresh(page);     // нужен @Transactional на метод, не работает с ForkJoinPool
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

    private List<SiteConfig> getSiteConfigs(List<Site> sitesList) {
        List<SiteConfig> siteConfigs = new ArrayList<>();
        for (Site site : sitesList) {
            SiteConfig siteConfig = new SiteConfig();
            siteConfig.setName(site.getName());
            siteConfig.setMillis(site.getMillis());
            try {
                URL url = new URL(site.getUrl());
                siteConfig.setUrl(url);
            } catch (MalformedURLException e) {
                throw new OkError("Некорректный url: ".concat(site.getUrl()));
            }
            siteConfigs.add(siteConfig);
        }
        return siteConfigs;
    }

    private Site isUrlInConfig(URL url){
        String uriHost = url.getHost().startsWith("www.") ? url.getHost().substring(4) : url.getHost();
        List<Site> sitesList = sites.getSites();
        return sitesList.stream().filter(s -> s.getUrl().contains(uriHost)).findAny().orElse(null);
    }

}
