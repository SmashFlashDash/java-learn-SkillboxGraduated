package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.classes.LemmaFinder;
import searchengine.classes.SiteIndexingTask;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
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
    private final Set<SiteIndexingTask> runningTasks = Collections.synchronizedSet(new HashSet<>());
    private final LemmaFinder lf;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    // TODO: indexingService можно внедриь в task Autowired сделав SCope prototype
    @Override
    public IndexingResponse startIndexingSites() {
        List<Site> sitesList = sites.getSites();
        if (!runningTasks.isEmpty()) {
            logger.error(String.format("Индексация уже запущена: %s", runningTasks));
            return new IndexingResponse(false, "Индексация уже запущена");
        } else if (sitesList.isEmpty()) {
            logger.error(String.format("В конфиге не указаны сайты: %s", runningTasks));
            return new IndexingResponse(false, "В конфигурации не указаны сайты для индексировния");
        }

        List<Thread> threads = new ArrayList<>();
        for (Site site : sitesList) {
            SiteEntity siteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXING);

            SiteIndexingTask task;
            try {
                URL url = new URL(site.getUrl());
                task = new SiteIndexingTask(url, siteEntity, site.getMillis(), jsoupConfig, this, lf);
            } catch (MalformedURLException e) {
                runningTasks.clear();
                return new IndexingResponse(false, "Некорректный url: ".concat(site.getUrl()));
            }

            threads.add(new Thread(() -> {
                runningTasks.add(task);
                siteRepository.deleteByName(site.getName());
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
            return new IndexingResponse(false, "Индексация не запущена");
        }
        runningTasks.forEach(SiteIndexingTask::stopCompute);
//        runningTasks.forEach(t -> { t.stopCompute(); runningTasks.remove(t); });
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse pageIndexing(String urlString) {
        // можно вынести JsoupConfig millis в dto
        if (!runningTasks.isEmpty()) {
            return new IndexingResponse(false, "Индексация запущена");
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return new IndexingResponse(false, "Некорректный url: ".concat(urlString));
        }

        String uriHost = url.getHost().startsWith("www.") ? url.getHost().substring(4) : url.getHost();
        List<Site> sitesList = sites.getSites();
        Site site = sitesList.stream().filter(s -> s.getUrl().contains(uriHost)).findAny().orElse(null);
        if (site == null) {
            return new IndexingResponse(false, "Сайт не задан в конфигурации");
        }

        SiteEntity siteEntity = siteRepository.findByName(site.getName());
        if (siteEntity == null) {
            siteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXED);
        }
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity != null) {
            // TODO: при стирании page lemma уменьшает frequency на 1 если frequency -1 > 0
            // List<LemmaEntity> lemmasToDelte = new ArrayList<>();
            // List<LemmaEntity> lemmasToUpdate = pageEntity.getLemmas().stream().filter(lemma -> {
            //     int newFrequency = lemma.getFrequency() - 1;
            //     if (newFrequency > 0) {
            //         lemma.setFrequency(newFrequency);
            //         return true;
            //     } else {
            //         lemmasToDelte.add(lemma);
            //         return false;
            //     }
            // }).collect(Collectors.toList());
            // pageRepository.delete(pageEntity);
            // lemmaRepository.deleteAll(lemmasToDelte);
            // lemmaRepository.saveAll(lemmasToUpdate);

            // TODO: получить все id и где frequncy = 1 удалить у остальных - 1
            //  через nativeQuery в LemmaEntity туда передать lemmas от page
            List<Long> lemmaIds = pageEntity.getLemmas().stream().map(LemmaEntity::getId).collect(Collectors.toList());
            lemmaRepository.deleteOneFrequencyLemmaByIndexes(lemmaIds);
            lemmaRepository.updateBeforeDeleteIndexes(lemmaIds);
            pageRepository.delete(pageEntity);

            // TODO: можно сделать через триггер в БД или EntityListener на IndexEntity @PreRemove
            //  и заинжектить туда indexRepository, создавать бином через config?
        }
        pageEntity = new PageEntity();
        pageEntity.setPath(url.toString());
        pageEntity.setSite(siteEntity);

        Document doc;
        try {
            doc = jsoupConfig.getJsoupDocument(url.toString());
            pageEntity.setContent(doc.outerHtml());
            pageEntity.setCode(doc.connection().response().statusCode());
            saveSite(siteEntity);
            savePage(pageEntity);
        } catch (HttpStatusException e) {
            pageEntity.setContent("");
            pageEntity.setCode(e.getStatusCode());
            savePage(pageEntity);
            return new IndexingResponse(true);
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            return new IndexingResponse(false, "Ошибка url: " + url.toString());
        } catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
            return new IndexingResponse(false, "Ошибка");
        }

        Map<String, Integer> lemmas = lf.collectLemmas(doc.text());
        saveLemmasMap(pageEntity, lemmas);

        return new IndexingResponse(true);
    }

    public Integer countPages() {
        return pageRepository.findAll().size();
    }

    // TODO: вылетает ошибка но не падает при сейве определенных Entity
    public synchronized void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public synchronized void savePage(PageEntity page) {
        logger.info("Вставить в БД: ".concat(page.getPath()));
        pageRepository.save(page);
    }

    public boolean isPageExistByPath(String path) {
        return pageRepository.existsByPath(path);
    }

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

}
