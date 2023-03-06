package searchengine.classes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupConfig;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class PageIndexingTask {

    public PageIndexingTask() {
    }

    public Map<String, Integer> ss(URL url, SiteEntity site, JsoupConfig jsoupConfig) throws IOException {
        // сущность страницы
        PageEntity page = new PageEntity();
        page.setPath(url.toString());
        page.setSiteId(site.getId());
        // подключение
        Connection.Response res;
        Document doc;
        res = Jsoup.connect(url.toString())
                .userAgent(jsoupConfig.getUserAgent())
                .referrer(jsoupConfig.getReffer())
                .timeout(jsoupConfig.getSocketTimeout())
                .method(Connection.Method.GET)
                .execute();
        doc = res.parse();

        //засэйвить страницу
        page.setContent(doc.outerHtml());
        page.setCode(doc.connection().response().statusCode());
        //indexingService.saveSite(site);
        //indexingService.savePage(page);
        //indexingUrisSet.remove(url.toString());

        return Collections.emptyMap();
    }

}
