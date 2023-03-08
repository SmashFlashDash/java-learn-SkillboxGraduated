package searchengine.classes;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.JsoupConfig;
import searchengine.controllers.ApiController;
import searchengine.model.EnumSiteStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

//@Component
//@Scope
//@RequiredArgsConstructor
public class SiteIndexingTask extends RecursiveTask<Boolean> {

    // TODO: можно ли внедрить service и config не делая класс Component
    //  или сделать компонентом и настроить Scope(prototype)
    private final IndexingService indexingService;
    private final JsoupConfig jsoupConfig;
    private final SiteEntity site;
    private final Integer millis;
    private final Set<String> indexingUrisSet;
    private final String uriHost;
    private final AtomicBoolean run;
    private final AtomicBoolean isComputed;
    private final URL url;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    /**
     * Создать рекурсивную задачу
     *
     * @param url             - исходный парсящийся uri
     *                        сэйввится как uriHost строкой, чтобы фильтровать сайты не относящиеся к домену
     * @param site            - сущность в БД сайтай
     * @param jsoupConfig     - получать настройки Jsoup из конфига
     * @param indexingService - сервис для записи статусов Site и новых Page
     */
    public SiteIndexingTask(URL url, SiteEntity site, Integer millis, JsoupConfig jsoupConfig, IndexingService indexingService) {
        this.url = url;
        String uriHost = this.url.getHost();
        this.uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        this.indexingService = indexingService;
        this.jsoupConfig = jsoupConfig;
        this.site = site;
        this.millis = millis;
        this.indexingUrisSet = Collections.synchronizedSet(new HashSet<String>());
        this.run = new AtomicBoolean(true);
        this.isComputed = new AtomicBoolean(false);
    }

    /**
     * конструктор используется в compute()
     */
    private SiteIndexingTask(URL url, SiteIndexingTask siteIndexingTask) {
        this.url = url;
        this.uriHost = siteIndexingTask.uriHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.jsoupConfig = siteIndexingTask.jsoupConfig;
        this.site = siteIndexingTask.site;
        this.millis = siteIndexingTask.millis;
        this.indexingUrisSet = siteIndexingTask.indexingUrisSet;
        this.run = siteIndexingTask.run;
        this.isComputed = siteIndexingTask.isComputed;
    }

    public void stopCompute() {
        if (!isDone()) {
            run.set(false);
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            indexingService.saveSite(site);
        }
    }

    @Override
    protected Boolean compute() {
        // проверка что не обрабаытвается в другом потоке и добавляем в set
        if (!indexingUrisSet.add(url.toString())) {
            return true;
        } else if (!run.get()) {
            return false;
        }
        UrlType uriType = validateUrl();
        if (!(uriType == UrlType.SITE_PAGE)) {
            //indexingUrisSet.remove(url.toString());
            return true;
        }


        // сущность страницы
        PageEntity page = new PageEntity();
        page.setPath(url.toString());
        page.setSite(site);
        Document doc;
        try {
            doc = jsoupConfig.getJsoupDocument(url.toString(), millis);
            page.setContent(doc.outerHtml());
            page.setCode(doc.connection().response().statusCode());
            // site.getPages().add(page);
            indexingService.savePage(page);
            indexingService.saveSite(site);
            indexingUrisSet.remove(url.toString());
        } catch (HttpStatusException e) {
            page.setContent(e.getMessage());
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            indexingService.saveSite(site);
            indexingUrisSet.remove(url.toString());
            return true;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            // indexingUrisSet.remove(url.toString());
            return true;
        } catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e)
            logger.error(e.getClass().getName() + ":" + e.getMessage() + " --- " + url.toString());
            run.set(false);
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName() + ":" + e.getMessage() + " --- " + url.toString());
            indexingService.saveSite(site);
            indexingUrisSet.remove(url.toString());
            return false;
        }


        try {
            LemmaFinder lf = LemmaFinder.getInstance();
            Map<String, Integer> lemmas = lf.collectLemmas(doc.text());
            indexingService.savePageLemmas(page, lemmas);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // обход ссылок и вернуть результат по ним
        List<SiteIndexingTask> tasks = walkSiteLinks(doc);
        return tasks.stream().allMatch(ForkJoinTask::join);
    }

    private UrlType validateUrl() {
        if (!(url.getHost().equals(uriHost) || url.getHost().endsWith(".".concat(uriHost)))) {
            return UrlType.OTHER_SITE;
        } else if (url.getPath().contains(".") && !url.getPath().endsWith(".html")) {
            // logger.warn(String.format("File: %s", uri.toString()));
            return UrlType.SITE_FILE;
        } else if (indexingService.isPageExistByPath(url.toString())) {
            return UrlType.PAGE_IN_TABLE;
        } else {
            return UrlType.SITE_PAGE;
        }
    }

    private Document jsoupGetDocument(PageEntity page) throws IOException {
        if (millis != null) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()));
            }
        }
        try {
            return Jsoup.connect(url.toString())
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReffer())
                    .timeout(jsoupConfig.getSocketTimeout())
                    .method(Connection.Method.GET)
                    .execute().parse();
        } catch (HttpStatusException e) {
            page.setContent("");
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            throw e;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            // logger.warn(e.getClass().getName().concat(": ").concat(uri.toString()));
            throw e;
        }
        catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
            logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            run.set(false);
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            indexingService.saveSite(site);
            throw e;
        }
    }

    private List<SiteIndexingTask> walkSiteLinks(Document doc) {
        List<SiteIndexingTask> tasks = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String uriString = link.attr("abs:href");
            URL newUri;
            try {
                newUri = new URL(uriString);
                newUri = new URL(newUri.getProtocol(), newUri.getHost(), newUri.getPath());
            } catch (MalformedURLException e) {
                continue;
            }
            SiteIndexingTask task = new SiteIndexingTask(newUri, this);
            tasks.add(task);
            task.fork();
        }
        return tasks;
    }


}
