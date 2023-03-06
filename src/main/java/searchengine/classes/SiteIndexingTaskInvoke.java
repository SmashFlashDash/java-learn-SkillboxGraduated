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
public class SiteIndexingTaskInvoke extends RecursiveTask<Boolean> {

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
    Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final URL url;

    /**
     * Создать рекурсивную задачу
     *
     * @param url             - исходный парсящийся uri
     *                        сэйввится как uriHost строкой, чтобы фильтровать сайты не относящиеся к домену
     * @param site            - сущность в БД сайтай
     * @param jsoupConfig     - получать настройки Jsoup из конфига
     * @param indexingService - сервис для записи статусов Site и новых Page
     */
    public SiteIndexingTaskInvoke(URL url, SiteEntity site, Integer millis, JsoupConfig jsoupConfig, IndexingService indexingService) {
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
    private SiteIndexingTaskInvoke(URL url, SiteIndexingTaskInvoke siteIndexingTask) {
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

    /**
     * когда дойдет до последнего потока и не создат новых задач все обьекты в subTasks будут Done
     * это считается что индексакция завершена
     */
    @Override
    protected Boolean compute() {
        // проверка что не обрабаытвается в другом потоке и добавляем в set
        if (!indexingUrisSet.add(url.toString())) {
            return true;
        }
        if (!run.get()) {
            return false;
        }

        UriType uriType;
//        if (!uri.getProtocol().startsWith("http")) {
//            uriType = UriType.NOT_LINK;
//        } else
         if (!(url.getHost().equals(uriHost) || url.getHost().endsWith(".".concat(uriHost)))) {
            uriType = UriType.OTHER_SITE;
        } else if (url.getPath().contains(".") && !url.getPath().endsWith(".html")) {
            uriType = UriType.SITE_FILE;
            // logger.warn(String.format("File: %s", uri.toString()));
        } else if (indexingService.isPageExistByPath(url.toString())) {
            uriType = UriType.PAGE_IN_TABLE;
        } else {
            uriType = UriType.SITE_PAGE;
        }
        if (!(uriType == UriType.SITE_PAGE)) {
            indexingUrisSet.remove(url.toString());
            return true;
        }
        // пауза на connection в соответсвии с конфигом
        if (millis != null) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()));
            }
        }

        // сущность страницы
        PageEntity page = new PageEntity();
        page.setPath(url.toString());
        page.setSiteId(site.getId());
        // подключение
        Connection.Response res;
        Document doc;
        try {
            res = Jsoup.connect(url.toString())
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReffer())
                    .timeout(jsoupConfig.getSocketTimeout())
                    .method(Connection.Method.GET)
                    .execute();
            doc = res.parse();
        }
        catch (HttpStatusException e) {
            page.setContent("");
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            indexingUrisSet.remove(url.toString());
            return true;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            // logger.warn(e.getClass().getName().concat(": ").concat(uri.toString()));
            indexingUrisSet.remove(url.toString());
            return true;
        }
        // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
        catch (IOException e) {
            logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            run.set(false);
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
            indexingService.saveSite(site);
            indexingUrisSet.remove(url.toString());
            return false;
        }
        //засэйвить страницу
        page.setContent(doc.outerHtml());
        page.setCode(doc.connection().response().statusCode());
        indexingService.saveSite(site);
        indexingService.savePage(page);
        indexingUrisSet.remove(url.toString());

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

        // обход ссылок и вернуть результат по ним
        List<SiteIndexingTaskInvoke> tasks = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String uriString = link.attr("abs:href");
            URL newUri;
            try {
                newUri = new URL(uriString);
                newUri = new URL(newUri.getProtocol(), newUri.getHost(), newUri.getPath());
            } catch (MalformedURLException e) {
                continue;
            }
            SiteIndexingTaskInvoke task = new SiteIndexingTaskInvoke(newUri, this);
            tasks.add(task);
            task.fork();
        }
        return tasks.stream().allMatch(ForkJoinTask::join);
    }
}
