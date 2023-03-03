package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface IndexingService {
    IndexingResponse startIndexingSites();
    IndexingResponse stopIndexingSites();
    void saveSite(SiteEntity siteEntity);
    void savePage(PageEntity page);
    boolean isPageExistByPath(String path);
    Integer countPages();
}
