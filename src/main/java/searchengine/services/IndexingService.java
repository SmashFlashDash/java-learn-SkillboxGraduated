package searchengine.services;

import searchengine.config.Site;
import searchengine.model.EnumSiteStatus;

import java.util.List;

public interface IndexingService {
    void startIndexingSites();
    void stopIndexingSites();
    void deleteDataBySites(List<String> siteNames);
    void saveSite(Site site, EnumSiteStatus status);
}
