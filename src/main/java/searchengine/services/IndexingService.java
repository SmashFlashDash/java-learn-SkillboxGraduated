package searchengine.services;

import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface IndexingService {
    StartIndexingResponse startIndexingSites();
    boolean stopIndexingSites();
    void deleteDataBySites(List<String> siteNames);
    void saveSite(SiteEntity siteEntity);
    void savePage(PageEntity page);
    boolean isPageExistByPath(String path);
    Integer countPages();
}
