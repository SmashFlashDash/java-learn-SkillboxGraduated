package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.classes.LemmaFinder;
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

import java.io.IOException;
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

    // TODO: indexingService можно внедриь в task Autowired сделав SCope prototype
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
    public IndexingResponse pageIndexing(String urlString) {
        // TODO: должен ли рабоать когда запущена индексация
        //  вернуть ошибку если домен сайта не из Jsoup конфига
        //  можно ли в Jsoup config сразу сделать URI из String
        // должен ли SiteIndexingResponse включать функционал PageIndexing
        //  default значение в Entity таблицы lemma
        // в таблицу lemma добавляются связки lemma страница
        if (!indexingTasks.isEmpty()) {
            return new IndexingResponse(false, "Индексация запущена");
        }


        URL url;
        try {
            url = new URL(urlString);
            // UrlType urlType;
            // if (!(url.getHost().equals(uriHost) || url.getHost().endsWith(".".concat(uriHost)))) {
            //     return UrlType.OTHER_SITE;
            // } else if (url.getPath().contains(".") && !url.getPath().endsWith(".html")) {
            //     // logger.warn(String.format("File: %s", uri.toString()));
            //     return UrlType.SITE_FILE;
            // } else if (indexingService.isPageExistByPath(url.toString())) {
            //     return UrlType.PAGE_IN_TABLE;
            // } else {
            //     return UrlType.SITE_PAGE;
            // }
        } catch (MalformedURLException e) {
            return new IndexingResponse(false, "Некорректный url: ".concat(urlString));
        }
        // String uriHost = url.getHost();
        // uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        final String uriHost = url.getHost().startsWith("www.") ? url.getHost().substring(4) : url.getHost();
        List<Site> sitesList = sites.getSites();
        Site site = sitesList.stream().filter(s -> s.getUrl().contains(uriHost)).findAny().orElse(null);
        if (site == null) {
            return new IndexingResponse(false, "Сайт не задан в конфигурации");
        }
        // TODO: можно вынести в класс парсер куда передается url, domain, конфиг Jsoup, сделать его scope prototype
        //  и использовать его в invoke

        // найти siteEntity по обьекту site можно вынести в метод создать обьект site
        // если нет создать
        SiteEntity siteEntity = siteRepository.findByName(site.getName());
        if (siteEntity == null) {
            siteEntity = new SiteEntity(site.getName(), site.getUrl(), EnumSiteStatus.INDEXED);
            saveSite(siteEntity);   // обнновить время статуса
        } else {
            // siteEntity.setStatus(EnumSiteStatus.INDEXING);
        }

        // создадим страницу для индексации
        // также ее надо найти и заменяь параметры
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity == null) {
            pageEntity = new PageEntity();
            pageEntity.setPath(url.toString());
            pageEntity.setSiteId(siteEntity.getId());
        }

        Document doc;
        try {
            doc = jsoupConfig.getJsoupDocument(url.toString());
            pageEntity.setContent(doc.outerHtml());
            pageEntity.setCode(doc.connection().response().statusCode());
            saveSite(siteEntity);   // обнновить время статуса
            savePage(pageEntity);   // засейвить страницу
        } catch (HttpStatusException e) {
            pageEntity.setContent("");
            pageEntity.setCode(e.getStatusCode());
            savePage(pageEntity);
            return new IndexingResponse(false, "Ошибка в переданои url");
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            // logger.warn(e.getClass().getName().concat(": ").concat(uri.toString()));
            return new IndexingResponse(false, "Ошибка в переданои url");
        } catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
            // logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            siteEntity.setStatus(EnumSiteStatus.FAILED);
            siteEntity.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            saveSite(siteEntity);
            return new IndexingResponse(false, "Ошибка в переданои url");
        }

        // TODO: получить текст с элементов без тэгов
        try {
            LemmaFinder lf = LemmaFinder.getInstance();
            // Map<String, Integer> lemmas = lf.collectLemmas(doc.select("p, ul, li, br, div, h1, h2, h3, h4, h5, h6").text());
            Map<String, Integer> lemmas = lf.collectLemmas(doc.text());

            // TODO: в базу вставить batch
            //  это надо делать уже в методе сервиса, с авто генерацией id и меод должен быть sycnhronized
        } catch (IOException e) {
            e.printStackTrace();
        }


//            SiteEntity ss = new SiteEntity();
//            for (Site s : sites.getSites()) {
//                if (s.getUrl().contains(uri.getHost())) {
//                    siteRepository.findAllByNameIn(Arrays.asList(s.getName()));
//                    siteRepository.findByName(s.getName());
//
//                    ss.setName(s.getName());
//                    ss.setUrl(s.getUrl());
//                    break;
//                }
//            }


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

    public void savePageLemmas(PageEntity pageEntity, Map<String, Integer> lemmas) {
//        lemmas.entrySet().stream();
//        for (Entry<String, Integer> lem : lemmas.entrySet()) {
//
//        }
    }

}
