package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Map;

public interface IndexingService {
    IndexingResponse startSitesIndexing();
    IndexingResponse stopIndexingSites();
    IndexingResponse pageIndexing(String url);
}
