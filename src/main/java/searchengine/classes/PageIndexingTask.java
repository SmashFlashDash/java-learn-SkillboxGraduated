package searchengine.classes;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import searchengine.config.JsoupConfig;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.EnumSiteStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PageIndexingTask {


//    public static Boolean parsePage(URL url, JsoupConfig jsoupConfig, SiteEntity siteEntity, PageEntity pageEntity) {
//        try {
//            Document doc = jsoupConfig.getJsoupDocument(url.toString());
//            pageEntity.setContent(doc.outerHtml());
//            pageEntity.setCode(doc.connection().response().statusCode());
//            saveSite(siteEntity);   // обнновить время статуса
//            savePage(pageEntity);   // засейвить страницу
//        } catch (HttpStatusException e) {
//            pageEntity.setContent("");
//            pageEntity.setCode(e.getStatusCode());
//            savePage(pageEntity);
//            return
//        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
//            // logger.warn(e.getClass().getName().concat(": ").concat(uri.toString()));
//            return new IndexingResponse(false, "Ошибка в переданои url");
//        }
//        catch (IOException e) { // catch (SocketTimeoutException | SocketException | UnknownHostException e) {
//            // logger.error(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
//            siteEntity.setStatus(EnumSiteStatus.FAILED);
//            siteEntity.setLastError(e.getClass().getName().concat(": ").concat(e.getMessage()).concat(" --- ").concat(url.toString()));
//            saveSite(siteEntity);
//            return new IndexingResponse(false, "Ошибка в переданои url");
//        }
//    }
}
