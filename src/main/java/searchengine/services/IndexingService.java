package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse sitesIndexing() throws IndexingServiceException;

    IndexingResponse stopIndexingSites() throws IndexingServiceException;

    IndexingResponse pageIndexing(String url) throws IndexingServiceException;

    boolean isIndexing();
}
