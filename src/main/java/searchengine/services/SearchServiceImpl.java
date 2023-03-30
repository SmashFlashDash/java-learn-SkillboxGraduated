package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.stereotype.Service;
import searchengine.config.LemmaFinder;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.search.SnippetParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final LemmaFinder lf;

    @Override
    public SearchResponse search(String query, Integer offset, Integer limit) {
        Set<String> lemmaSet = lf.getLemmaSet(query);
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInOrderByFrequencyAsc(lemmaSet);
        List<SearchData> searchDataList = getSearchPages(lemmas, lemmaSet);
        return new SearchResponse(true, searchDataList.size(), pageOfList(searchDataList, offset, limit));
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        Set<String> lemmaSet = lf.getLemmaSet(query);
        SiteEntity siteEntity = siteRepository.findByUrlEquals(site);
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInAndSiteEqualsOrderByFrequencyAsc(lemmaSet, siteEntity);
        List<SearchData> searchDataList = getSearchPages(lemmas, lemmaSet);
        return new SearchResponse(true, searchDataList.size(), pageOfList(searchDataList, offset, limit));
    }

    private <T> List<T> pageOfList(List<T> searchDataList, int offset, int limit) {
        PagedListHolder<T> pages = new PagedListHolder<>(searchDataList);
        pages.setPageSize(limit);
        pages.setPage(offset);
        return pages.getPageList();
    }

    private List<SearchData> getSearchPages(List<LemmaEntity> lemmas, Set<String> lemmaSet) {
        TreeSet<SearchData> searchDataList = new TreeSet<>(Comparator
                .comparing(SearchData::getRelevance)
                .thenComparing(SearchData::getSnippet).reversed());

        HashMap<SiteEntity, Set<PageEntity>> pagesMap = new HashMap<>();
        lemmas.forEach(i -> {
            if (pagesMap.containsKey(i.getSite())) {
                pagesMap.get(i.getSite()).retainAll(i.getIndexes().stream().map(IndexEntity::getPage).collect(Collectors.toList()));
            } else {
                Set<PageEntity> list = i.getIndexes().stream().map(IndexEntity::getPage).collect(Collectors.toSet());
                pagesMap.put(i.getSite(), list);
            }
        });

        AtomicReference<Float> maxRelevance = new AtomicReference<>(0F);
        pagesMap.forEach((site, pages) -> {
            for (PageEntity page : pages) {
                Document doc = Jsoup.parse(page.getContent());
                SnippetParser pageSnippet = new SnippetParser(doc, lf, lemmaSet);

                SearchData data = new SearchData();
                data.setSite(site.getUrl());
                data.setSiteName(site.getName());
                // TODO: костыль, frontend скаладывает SiteUrl и Path
                data.setUri(page.getPath().length() > site.getUrl().length() ?
                        page.getPath().substring(site.getUrl().length()) :
                        page.getPath());
                data.setTitle(doc.title());
                data.setSnippet(pageSnippet.getSnippet());
                float relevance = indexRepository.findAllByPageAndLemmaIn(page, lemmas).stream().map(IndexEntity::getRank).reduce(0F, Float::sum);
                if (maxRelevance.get() < relevance){
                    maxRelevance.set(relevance);
                }
                data.setRelevance(relevance);
                searchDataList.add(data);
            }
        });
        searchDataList.forEach(data -> data.setRelevance(maxRelevance.get() / data.getRelevance()));
        return new ArrayList<>(searchDataList);
    }
}

