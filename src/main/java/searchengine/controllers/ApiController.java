package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.error.ErrorResponse;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("/api")
public class ApiController {

    //TODO сделать lemmaFinder componentSpring
    // занижектить в indexingService

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService,
                         @Qualifier("indexingServiceImpl") IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing(){
        return ResponseEntity.ok(indexingService.startSitesIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing(){
        return ResponseEntity.ok(indexingService.stopIndexingSites());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> pageIndexing(@RequestParam(name = "url") String url){
        return ResponseEntity.ok(indexingService.pageIndexing(url));
    }

    @GetMapping
    public ResponseEntity<Object> search(@PathVariable(name = "query") String query,
                                                   @PathVariable(name = "site", required = false) String site,
                                                   @PathVariable(name = "offset", required = false) Integer offset,
                                                   @PathVariable(name = "limit", required = false) Integer limit) {
        if (site != null) {
            try {
                URL url = new URL(site);
            } catch (MalformedURLException e) {
                return ResponseEntity.ok(new ErrorResponse(false, "Ошибка Url: " + site));
            }
        }
        offset = offset == null ? 0 : offset;
        limit = limit == null ? 20 : limit;


        return ResponseEntity.ok(new ErrorResponse(false, ""));
    }
}
