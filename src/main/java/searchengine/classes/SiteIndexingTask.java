package searchengine.classes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.concurrent.RecursiveAction;

@Component
public class SiteIndexingTask extends RecursiveAction {
    // TODO: нужно иметь доступ к сервайсу indexing
    //  сервайс должен уметь сохранять страницы в таблицу и полчать
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @Autowired
    public SiteIndexingTask(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @Override
    protected void compute() {

    }
}
