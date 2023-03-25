package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startSitesIndexing();

    IndexingResponse stopIndexingSites();

    IndexingResponse pageIndexing(String url);
}
