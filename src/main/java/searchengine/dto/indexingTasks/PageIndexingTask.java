package searchengine.dto.indexingTasks;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupConfig;
import searchengine.model.EnumSiteStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.services.IndexingServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageIndexingTask extends AbstractIndexingTask {
    private final IndexingServiceImpl indexingService;
    private final JsoupConfig jsoupConfig;
    private final SiteEntity site;
    private final AtomicBoolean run;
    private final URL url;
    private final LemmaFinder lf;

    public PageIndexingTask(URL url, SiteEntity site, JsoupConfig jsoupConfig,
                            IndexingServiceImpl indexingService, LemmaFinder lf) {
        this.url = url;
        this.indexingService = indexingService;
        this.jsoupConfig = jsoupConfig;
        this.site = site;
        this.run = new AtomicBoolean(true);
        this.lf = lf;
    }

    @Override
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
        if (!run.get()) {
            return false;
        }
        PageEntity page = new PageEntity();
        page.setPath(url.toString());
        page.setSite(site);
        Document doc;
        try {
            doc = jsoupConfig.getJsoupDocument(url.toString());
            page.setContent(doc.outerHtml());
            page.setCode(doc.connection().response().statusCode());
            indexingService.savePage(page);
            indexingService.saveSite(site);
        } catch (HttpStatusException e) {
            page.setContent(e.getMessage());
            page.setCode(e.getStatusCode());
            indexingService.savePage(page);
            return true;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            return false;
        } catch (IOException e) {
            site.setStatus(EnumSiteStatus.FAILED);
            site.setLastError(e.getClass().getName() + ":" + e.getMessage());
            indexingService.saveSite(site);
            return false;
        }

        Map<String, Integer> lemmas = lf.collectLemmas(doc.text());
        synchronized (PageIndexingTask.class) {
            indexingService.saveLemmasMap(page, lemmas);
        }
        return true;
    }

}
