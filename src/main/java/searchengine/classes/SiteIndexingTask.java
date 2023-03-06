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
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

//@Component
//@Scope
//@RequiredArgsConstructor
public class SiteIndexingTask extends RecursiveAction {

    // TODO: можно ли внедрить service и config не делая класс Component
    //  или сделать компонентом и настроить Scope(prototype)
    private final IndexingService indexingService;
    private final JsoupConfig jsoupConfig;
    private final SiteEntity site;
    private final Integer millis;
    private final Set<String> indexingUrisSet;
    private final String uriHost;
    private URI uri;
    private final AtomicBoolean run;
    private final Set<SiteIndexingTask> subTasks;
    private final AtomicBoolean isComputed;
    private Runnable computeFinishedAction; // здесь сейвистся lambda function
    private Runnable stopComputeAction; // здесь сейвистся lambda function
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    /**
     * Создать рекурсивную задачу
     * @param uri   - исходный парсящийся uri
     *              сэйввится как uriHost строкой, чтобы фильтровать сайты не относящиеся к домену
     * @param site - сущность в БД сайтай
     * @param jsoupConfig   - получать настройки Jsoup из конфига
     * @param indexingService - сервис для записи статусов Site и новых Page
     */
    public SiteIndexingTask(String uri, SiteEntity site, Integer millis, JsoupConfig jsoupConfig, IndexingService indexingService) {
        this.uri = URI.create(uri);
        String uriHost = this.uri.getHost();
        this.uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        this.indexingService = indexingService;
        this.jsoupConfig = jsoupConfig;
        this.site = site;
        this.millis = millis;
        this.indexingUrisSet = Collections.synchronizedSet(new HashSet<String>());
        this.run = new AtomicBoolean(true);
        this.isComputed = new AtomicBoolean(false);
        this.subTasks = Collections.synchronizedSet(new HashSet<SiteIndexingTask>());
    }

    /**
     * конструктор используется в compute()
     */
    private SiteIndexingTask(URI uri, SiteIndexingTask siteIndexingTask) {
        this.uri = uri;
        this.uriHost = siteIndexingTask.uriHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.jsoupConfig = siteIndexingTask.jsoupConfig;
        this.site = siteIndexingTask.site;
        this.millis = siteIndexingTask.millis;
        this.indexingUrisSet = siteIndexingTask.indexingUrisSet;
        this.run = siteIndexingTask.run;
        this.isComputed = siteIndexingTask.isComputed;
        this.subTasks = siteIndexingTask.subTasks;
        this.computeFinishedAction = siteIndexingTask.computeFinishedAction;
        this.stopComputeAction = siteIndexingTask.stopComputeAction;
        subTasks.add(this);
    }

    public SiteEntity getSiteEntity() {
        return site;
    }

    public void setComputedAction(Runnable exp){
        computeFinishedAction = exp;
    }

    public void setStopComputeAction(Runnable exp){
        stopComputeAction = exp;
    }

    public void stopCompute() {
        run.set(false);
    }

    /**
     * когда дойдет до последнего потока и не создат новых задач все обьекты в subTasks будут Done
     * это считается что индексакция завершена
     */
    private void isComputeFinished() {
        List<SiteIndexingTask> tasksDone = subTasks.stream().filter(ForkJoinTask::isDone).collect(Collectors.toList());
        subTasks.removeAll(tasksDone);
//        if (!run.get() || subTasks.isEmpty()){
        if (!run.get()) {
            // TODO: кидать excaption
            if (stopComputeAction != null) {
                stopComputeAction.run();
            }
            // TODO: это условие может и не срабоать
        } else if (subTasks.isEmpty() || (subTasks.size() == 1 && subTasks.contains(this))) {
            isComputed.set(true);
            if (computeFinishedAction != null) {
                computeFinishedAction.run();
            }
        }
    }

    @Override
    protected void compute() {
        uri = URI.create(String.format("%s://%s%s", uri.getScheme(), uri.getAuthority(), uri.getPath()));
        if (!indexingUrisSet.add(uri.toString())) {
            return;
        }

        UrlType uriType = urlValidate();
        if (!(uriType == UrlType.SITE_PAGE)) {
            indexingUrisSet.remove(uri.toString());
            return;
        }

        PageEntity page = new PageEntity();
        page.setPath(uri.toString());
        page.setSiteId(site.getId());

        try {
            if (millis != null) {
                Thread.sleep(millis);
            }
            Connection.Response res = Jsoup.connect(uri.toString())
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReffer())
                    .method(Connection.Method.GET)
                    .execute();
            Document doc = res.parse();

            page.setContent(doc.outerHtml());
            page.setCode(doc.connection().response().statusCode());
            indexingService.saveSite(site);
            indexingService.savePage(page);
            indexingUrisSet.remove(uri.toString());

            if (!run.get()) {
                // остановка пользователем
                stopComputeAction.run();
            } else {
                for (Element link : doc.select("a[href]")) {
                    URI newUri = URI.create(link.attr("abs:href"));
                    SiteIndexingTask task = new SiteIndexingTask(newUri, this);
                    task.fork();
                }
            }
            isComputeFinished();

            // TODO: если ошибка завершить все потоки, и поставить статус
        } catch (HttpStatusException e) {
            page.setContent("");
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            indexingUrisSet.remove(uri.toString());
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            logger.warn(e.getClass().getName().concat(": ").concat(uri.toString()));
        }
        // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
        catch (IOException | InterruptedException e) {
            // TODO: Ошибки можно вынести в setFunction
            logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()));
            stopCompute();
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()));
            indexingService.saveSite(site);
        }
    }

    // TODO: может быть слабое место что надо заносить ссылку в setUrls в начале compute, после отброса query
    //  затем если есть в таблце убрать из set
    private UrlType urlValidate() {
        if (!uri.getScheme().startsWith("http")) {
            return UrlType.NOT_LINK;
        } else if (!(uri.getHost().equals(uriHost) || uri.getHost().endsWith(".".concat(uriHost)))) {
            return UrlType.OTHER_SITE;
            // TODO: можено через uri.toUrl().getContent().getType(), но это делается в Jsoup
            // } else if (!(uri.getPath().endsWith("/") || uri.getPath().endsWith(".html"))) {
            //     return UriType.SITE_FILE;
        } else if (indexingService.isPageExistByPath(uri.toString())) {
            return UrlType.PAGE_IN_TABLE;
        }
        return UrlType.SITE_PAGE;
    }
}
