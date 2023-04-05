package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchServiceImpl searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws IndexingServiceException {
        return ResponseEntity.ok(indexingService.sitesIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() throws IndexingServiceException {
        return ResponseEntity.ok(indexingService.stopIndexingSites());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> pageIndexing(@RequestParam(name = "url") String url) throws IndexingServiceException {
        return ResponseEntity.ok(indexingService.pageIndexing(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(name = "query") String query,
                                                 @RequestParam(name = "site", required = false) String site,
                                                 @RequestParam(name = "offset", required = false) Integer offset,
                                                 @RequestParam(name = "limit", required = false) Integer limit) {
        offset = offset == null ? 0 : offset;
        limit = limit == null ? 20 : limit;
        if (site == null) {
            return ResponseEntity.ok(searchService.search(query, site, offset, limit));
        } else {
            return ResponseEntity.ok(searchService.search(query, offset, limit));
        }
    }
}
