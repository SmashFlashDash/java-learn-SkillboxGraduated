package searchengine.services.indexing;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.config.JsoupConfig;
import searchengine.config.LemmaFinder;
import searchengine.controllers.ApiController;
import searchengine.dto.indexing.SiteData;
import searchengine.model.EnumSiteStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.IndexingServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

//@Component
//@Scope
//@RequiredArgsConstructor
public class SiteIndexingTask extends AbstractIndexingTask {

    // TODO: можно ли внедрить service и config не делая класс Component
    //  или сделать компонентом и настроить Scope(prototype)
    private final IndexingServiceImpl indexingService;
    private final JsoupConfig jsoupConfig;
    private final SiteEntity site;
    private final SiteData siteData;
    private final Set<String> runningUrls;
    private final String uriHost;
    private final AtomicBoolean run;
    private final URL url;
    private final LemmaFinder lf;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    /**
     * Создать рекурсивную задачу
     * @param site            - сущность в БД сайтай
     * @param jsoupConfig     - получать настройки Jsoup из конфига
     * @param indexingService - сервис для записи статусов Site и новых Page
     */
    public SiteIndexingTask(SiteData siteData, SiteEntity site, JsoupConfig jsoupConfig,
                            IndexingServiceImpl indexingService, LemmaFinder lf) {
        this.siteData = siteData;
        this.url = siteData.getUrl();
        String uriHost = this.url.getHost();
        this.uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        this.indexingService = indexingService;
        this.jsoupConfig = jsoupConfig;
        this.site = site;
        this.runningUrls = Collections.synchronizedSet(new HashSet<String>());
        this.run = new AtomicBoolean(true);
        this.lf = lf;
    }

    /**
     * конструктор используется в compute()
     */
    private SiteIndexingTask(URL url, SiteIndexingTask siteIndexingTask) {
        this.siteData = siteIndexingTask.siteData;
        this.url = url;
        this.uriHost = siteIndexingTask.uriHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.jsoupConfig = siteIndexingTask.jsoupConfig;
        this.site = siteIndexingTask.site;
        this.runningUrls = siteIndexingTask.runningUrls;
        this.run = siteIndexingTask.run;
        this.lf = siteIndexingTask.lf;
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
        if (!runningUrls.add(url.toString())) {
            return true;
        } else if (!run.get()) {
            return false;
        }
        UrlType uriType = validateUrl();
        if (!(uriType == UrlType.SITE_PAGE)) {
            // indexingUrisSet.remove(url.toString());
            return true;
        }

        PageEntity page = new PageEntity();
        page.setPath(url.toString());
        page.setSite(site);
        Document doc;
        try {
            doc = jsoupConfig.getJsoupDocument(url.toString(), siteData.getMillis());
            page.setContent(doc.outerHtml());
            page.setCode(doc.connection().response().statusCode());
            // site.getPages().add(page);
            indexingService.savePage(page);
            indexingService.saveSite(site);
            runningUrls.remove(url.toString());
        } catch (HttpStatusException e) {
            page.setContent(e.getMessage());
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            indexingService.saveSite(site);
            runningUrls.remove(url.toString());
            return true;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            // runningUrls.remove(url.toString());
            return true;
        } catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e)
            // logger.error(e.getClass().getName() + ":" + e.getMessage());
            run.set(false);
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName() + ":" + e.getMessage());
            indexingService.saveSite(site);
            runningUrls.remove(url.toString());
            return false;
        }

        Map<String, Integer> lemmas = lf.collectLemmas(doc.text());
        synchronized (SiteIndexingTask.class) {
            indexingService.saveLemmasIndexes(page, lemmas);
        }

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
