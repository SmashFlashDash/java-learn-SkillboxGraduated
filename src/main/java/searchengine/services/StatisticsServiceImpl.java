package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final IndexingService indexingService;

    @Override
    public StatisticsResponse getStatistics() {
        List<SiteEntity> siteEntity = siteRepository.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteEntity.size());
        total.setIndexing(indexingService.isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (SiteEntity site : siteEntity) {
            total.setPages(total.getPages() + site.getPages().size());
            total.setLemmas(total.getLemmas() + site.getLemmas().size());

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(site.getPages().size());
            item.setLemmas(site.getLemmas().size());
            item.setStatus(site.getStatus().name());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().getTime());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
