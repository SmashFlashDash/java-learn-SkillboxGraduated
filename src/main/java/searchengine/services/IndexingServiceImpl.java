package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.classes.SiteIndexingTask;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final JsoupConfig jsoupConfig;
    private final ForkJoinPool forkJoinPool;
    private final List<SiteIndexingTask> sitesThreads = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    // TODO: в RecusiveAction нужно иметь доступ к сервайсу indexing
    //  сервайс должен уметь сохранять страницы в таблицу и полчуать
    //  можно сделать через интерфейс indexingService или передать repository чтобы меньше зависимость
    //  можно попробовать реализовать через Spring зависимостью
//    https://stackoverflow.com/questions/31757216/spring-cannot-propagate-transaction-to-forkjoins-recursiveaction

    /**
     * если сделать в SiteIndexingTask вызов задач invoke то выполняется медленно
     * @return
     */
    @Override
    public IndexingResponse startIndexingSites() {
        if (!sitesThreads.isEmpty()) {
            logger.error(String.format("Индексация уже запущена: %s", sitesThreads));
            return new IndexingResponse(false, "Индексация уже запущена");
        }

        List<Site> sitesList = sites.getSites();
        if (!sitesList.isEmpty()) {
            // TODO: вернуть кол-во удаленных записей @Modified
            // deleteDataBySites(sitesList.stream().map(Site::getName).collect(Collectors.toList()));
        } else {
            logger.error(String.format("В конфиге не указаны сайты: %s", sitesThreads));
            return new IndexingResponse(false, "В конфигурации не указаны сайты для индексировния");
        }

        for (Site site : sitesList) {
            SiteEntity siteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXING);
            saveSite(siteEntity);
            SiteIndexingTask task = new SiteIndexingTask(site.getUrl(), siteEntity, site.getMillis(), jsoupConfig, this);
            sitesThreads.add(task);

            // TODO: метод выполняемый когда закончится вычисление по сайту
            task.setComputedAction(() -> {
                logger.info(String.format("Парсинг сайта заврешен: %s -- %s", siteEntity.getName(), Thread.currentThread()));
                siteEntity.setStatus(EnumSiteStatus.INDEXED);
                saveSite(siteEntity);
                sitesThreads.remove(task);
            });
            task.setStopComputeAction(() -> {
                logger.info(String.format("Остановим таску: %s -- %s", siteEntity.getName(), Thread.currentThread()));
                siteEntity.setStatus(EnumSiteStatus.FAILED);
                siteEntity.setLastError("Индексация остановлена пользователем");
                saveSite(siteEntity);
                sitesThreads.remove(task);
            });
            forkJoinPool.execute(task);     // начать выплднять задачу и не ждать join
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndexingSites() {
        sitesThreads.forEach(SiteIndexingTask::stopCompute);
        sitesThreads.clear();
        return new IndexingResponse(true);
    }

    @Override
    public void saveSite(SiteEntity siteEntity) {
        siteRepository.save(siteEntity);
    }

    // TODO: можно сделать batch insert
    //  складывать все и flush как закончится парсинг
    //  но тогда проверять page в таблице надо еще и в batch
    @Override
    public void savePage(PageEntity page) {
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
