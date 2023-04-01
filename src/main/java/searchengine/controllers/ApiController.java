package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.SearchServiceImpl;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService,
                         @Qualifier("indexingServiceImpl") IndexingService indexingService,
                         SearchServiceImpl searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.sitesIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexingSites());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> pageIndexing(@RequestParam(name = "url") String url) {
        return ResponseEntity.ok(indexingService.pageIndexing(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(name = "query") String query,
                                                 @RequestParam(name = "site", required = false) String site,
                                                 @RequestParam(name = "offset", required = false) Integer offset,
                                                 @RequestParam(name = "limit", required = false) Integer limit) {
        offset = offset == null ? 0 : offset;
        limit = limit == null ? 20 : limit;
        if (site != null) {
            return ResponseEntity.ok(searchService.search(query, site, offset, limit));
        } else {
            return ResponseEntity.ok(searchService.search(query, offset, limit));
        }
    }
}
