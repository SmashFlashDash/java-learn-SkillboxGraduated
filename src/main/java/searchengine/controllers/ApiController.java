package searchengine.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    Logger logger = LoggerFactory.getLogger(ApiController.class);

    public ApiController(StatisticsService statisticsService,
                         @Qualifier("indexingServiceImplInvoke") IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing(){
//        indexingService.startIndexingSites();
//        try {
//            Thread.sleep(60_000);
//            indexingService.stopIndexingSites();
//            logger.info("В БД page: ".concat(String.valueOf(indexingService.countPages())));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return ResponseEntity.ok(true);
        return ResponseEntity.ok(indexingService.startIndexingSites());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Boolean> stopIndexing(){
        return ResponseEntity.ok(indexingService.stopIndexingSites());
    }
}
