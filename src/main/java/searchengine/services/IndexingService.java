package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse sitesIndexing();

    IndexingResponse stopIndexingSites();

    IndexingResponse pageIndexing(String url);

    boolean isIndexing();
}
