package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Map;

public interface IndexingService {
    IndexingResponse startIndexingSites();
    IndexingResponse stopIndexingSites();
    IndexingResponse pageIndexing(String url);
    void savePageLemmas(PageEntity page, Map<String, Integer> lemmas);
    void saveSite(SiteEntity siteEntity);
    void savePage(PageEntity page);
    boolean isPageExistByPath(String path);
    Integer countPages();
    IndexingResponse debug();
}
