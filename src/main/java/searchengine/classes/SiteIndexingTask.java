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
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.IndexingService;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RecursiveAction;

//@Component
//@Scope
//@RequiredArgsConstructor
// TODO: нужно ли делать callable или RecursiveTask чтобы обновлял интрефейс в процессе индексации
public class SiteIndexingTask extends RecursiveAction implements Callable<String> {

    // TODO: можно ли внедрить service и config не делая класс Component
    //  или сделать компонентом и настроить Scope(prototype)
    private final IndexingService indexingService;
    private final JsoupConfig jsoupConfig;
    private final SiteEntity site;
    private final Set<String> setUrls;
    private final String uriHost;
    Logger logger = LoggerFactory.getLogger(ApiController.class);
    private URI uri;

    public SiteIndexingTask(String uri, SiteEntity site, JsoupConfig jsoupConfig, IndexingService indexingService) {
        this.uri = URI.create(uri);
        String uriHost = this.uri.getHost();
        this.uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        this.indexingService = indexingService;
        this.jsoupConfig = jsoupConfig;
        this.site = site;
        this.setUrls = Collections.synchronizedSet(new HashSet<String>());
    }

    /**
     * передаем domain используется из compute()
     */
    private SiteIndexingTask(URI uri, SiteIndexingTask siteIndexingTask) {
        this.uri = uri;
        this.uriHost = siteIndexingTask.uriHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.jsoupConfig = siteIndexingTask.jsoupConfig;
        this.site = siteIndexingTask.site;
        this.setUrls = siteIndexingTask.setUrls;
    }

    @Override
    protected void compute() {
//        logger.info("Started indexing: " + uri);

        // ссылка без ? # параметров
        uri = URI.create(String.format("%s://%s%s", uri.getScheme(), uri.getAuthority(), uri.getPath()));
        //  проверить url соответсвует ли page
        UriType uriType = urlValidate();
        if (!(uriType == UriType.SITE_LINK || uriType == UriType.SITE_FILE)) {
//            logger.info("Не парсим " + urlType.toString() + ": " + uri.toString());
            return;
        }
        // TODO: нет ли ссылки в таблице || проверить парсится ли ссылка
        // можно добавить synchronized обьект на setUrls
        if (indexingService.isPageExistByPath(uri.toString())) {
//            logger.info("Не парсим ссылка в таблице: " + uri.toString());
            return;
        }
        if (!setUrls.add(uri.toString())) {
//            logger.info("Не парсим ссылка в сете: " + uri.toString());
            return;
        }

        PageEntity page = new PageEntity();
        page.setPath(uri.toString());
        page.setSiteId(site.getId());

//        Connection.Response res;
//        Document doc;
        try {
            Thread.sleep(100);
            Connection.Response res = Jsoup.connect(uri.toString())
                    .userAgent(jsoupConfig.getUserAgent())
                    .referrer(jsoupConfig.getReffer())
//                    .ignoreContentType(true)
                    .method(Connection.Method.GET)
                    .execute();
//            String type = res.contentType();
            Document doc = res.parse();

            page.setContent(doc.outerHtml());
            page.setCode(doc.connection().response().statusCode());
            indexingService.updateSiteTimeStatus(site);
            indexingService.savePage(page);
            setUrls.remove(uri.toString());

            if (uriType == UriType.SITE_LINK) {
                for (Element link : doc.select("a[href]")) {
                    URI newUri = URI.create(link.attr("abs:href"));
                    SiteIndexingTask task = new SiteIndexingTask(newUri, this);
                    task.fork();
                }
            }
        } catch (HttpStatusException e) {
            page.setContent("");
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            setUrls.remove(uri.toString());
        } catch (UnsupportedMimeTypeException e) {
            logger.info("File: ".concat(uri.toString()));
        } catch (IOException | InterruptedException e) {
            // TODO: установить статуст ошибки page
            e.printStackTrace();
        }
        // TODO: теперь надо как-то выяснить что обработка закончена
        //  это в ForkJoin task котоырй смотрит завершились ли потоки?

    }

    /**
     * вызвать сервис у которого аннотация transactional
     */
    private void writeToDB() {

    }

    @Override
    public String call() throws Exception {
        return null;
    }

    private UriType urlValidate() {
        if (!uri.getScheme().startsWith("http")) {
            return UriType.NOT_LINK;
        } else if (!(uri.getHost().equals(uriHost) || uri.getHost().endsWith(".".concat(uriHost)))) {
            return UriType.OTHER_SITE_LINK;
            // TODO: можено через uri.toUrl().getContent().getType(), но это делается в Jsoup
//        } else if (!(uri.getPath().endsWith("/") || uri.getPath().endsWith(".html"))) {
//            return UriType.SITE_FILE;
        }
        return UriType.SITE_LINK;
    }
}
