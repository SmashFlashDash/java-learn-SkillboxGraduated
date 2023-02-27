package searchengine.services;

import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;

public interface IndexingService {
    void startIndexingSites();
    void stopIndexingSites();
    void deleteDataBySites(List<String> siteNames);
    void saveSite(SiteEntity siteEntity);
    void savePage(PageEntity page);
    void siteSetStatusIndexingStopped(SiteEntity siteEntity);
    boolean isPageExistByPath(String path);
    void updateSiteTimeStatus(SiteEntity site);
}
