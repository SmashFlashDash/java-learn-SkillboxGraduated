package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.classes.LemmaFinder;
import searchengine.classes.SiteIndexingTask;
import searchengine.config.JsoupConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final JsoupConfig jsoupConfig;
    private final ForkJoinPool forkJoinPool;
    private final Set<SiteIndexingTask> indexingTasks = Collections.synchronizedSet(new HashSet<>());
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    public IndexingResponse debug(){
        // DEBUG Entitys connections
        SiteEntity siteEntity = siteRepository.findByName("bequiet.com");
        List<PageEntity> pages = siteEntity.getPages();
        List<LemmaEntity> lemmaEntities = siteEntity.getLemmas();

        List<IndexEntity> indexes = pages.get(0).getIndexes();
        // TODO: как из pages сразу получить все lemma через таблицу indexes
        //List<LemmaEntity> s = pages.get(0).getLemmas();

        LemmaEntity lemma = indexes.get(0).getLemma();
        // TODO: как из lemma получить сразу page через таблицу indexes
        //  если делать EmbedableId то пропадает основной Id
        //  если через свзять @OneToMany и @JoinTable меняется Primary key таблицы indexes
        //  а поле id перестает быть auto_increment
        PageEntity page1 = lemma.getIndexes().get(0).getPage();

        Boolean b = siteEntity == pages.get(0).getSite();
        Boolean b1 = siteEntity == lemmaEntities.get(0).getSite();
        // lemmaEntities.get(0).getPages();

        // TODO:
        //  - медленее ли cascade save, чем напрямуб через репозиторий
        //  - сделать работу index создав sql триггер:
        //  при убирании index, найти lemmaEntity и -1 значение frequency
        //  если значение == 1 дропнуть запись
        //  если функцией тогда можно сделать и при insert в index чтобы изменялось значнеие на +1
        //  -------
        //  если делать вручную то когда убираем page, брем по page все index, по каждому lemma -1 или drop
        //  стираем список index
        pages.add(new PageEntity(siteEntity, "www", 1, "www"));
        pages.add(new PageEntity(siteEntity,"wwww", 2, "www"));
        siteRepository.save(siteEntity);
        return new IndexingResponse(true);
        //---------------------------

    }

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

            SiteIndexingTask task;
            try {
                URL url = new URL(site.getUrl());
                task = new SiteIndexingTask(url, siteEntity, site.getMillis(), jsoupConfig, this);
            } catch (MalformedURLException e) {
                return new IndexingResponse(false, "Некорректный url: ".concat(site.getUrl()));
            }
            indexingTasks.add(task);

            threads.add(new Thread(() -> {
                siteRepository.deleteByName(site.getName());
                saveSite(siteEntity);
                try {
                    Boolean res = forkJoinPool.invoke(task);
                    if (res) {
                        siteEntity.setStatus(EnumSiteStatus.INDEXED);
                    }
                } catch (Exception e) {
                    siteEntity.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()));
                    siteEntity.setStatus(EnumSiteStatus.FAILED);
                }
                saveSite(siteEntity);
                indexingTasks.remove(task);
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
        indexingTasks.forEach(SiteIndexingTask::stopCompute);
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
            pageEntity.setSite(siteEntity);
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
            savePageLemmas(pageEntity, lemmas);
            // TODO: в базу вставить batch
            //  это надо делать уже в методе сервиса, с авто генерацией id и меод должен быть sycnhronized
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new IndexingResponse(false);
    }


    // TODO: вылетает ошибка но не падает при сейве определенных Entity
    @Override
    public synchronized void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public synchronized void savePage(PageEntity page) {
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
        // TODO: надо еще доабвлять в таблицу indexes связь page и lemma по id
        //  это связь many to many
        //  эти поля надо сделать cascade
        //  если сделать cascade не только remove а ALL
        //  то можно добавлять в список полученный pageEntity обьект lemmaEntity
        //  а сохранять будет при сохрании страницы page
        //  ----------
        //  чтобы проверить как работают entity заполнить таблицу mock данными
        //  и в дебаге порабоать с обьектами
        //  ----------
        //  чтобы не писать код на установку статуса в задачах возвращать не Boolean а Enum
        //  -
        //  -
        //  -
        //  - сделать работу index создав sql триггер:
        //  при убирании index, найти lemmaEntity и -1 значение frequency
        //  если значение == 1 дропнуть запись
        //  если функцией тогда можно сделать и при insert в index чтобы изменялось значнеие на +1
        //  -------
        //  если делать вручную то когда убираем page, брем по page все index, по каждому lemma -1 или drop
        //  стираем список index


        //List<LemmaEntity> s = pageEntity.getLemmaEntity();
        List<LemmaEntity> lemmaEntities = lemmas.entrySet().stream().map(lem -> {
            LemmaEntity lemmaEntity = lemmaRepository.findBySiteIdAndLemma(pageEntity.getSite().getId(), lem.getKey());
            if (lemmaEntity == null) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(pageEntity.getSite());
                lemmaEntity.setLemma(lem.getKey());
                lemmaEntity.setFrequency(lem.getValue());
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            return lemmaEntity;
        }).collect(Collectors.toList());
        lemmaRepository.saveAll(lemmaEntities);
    }

}
