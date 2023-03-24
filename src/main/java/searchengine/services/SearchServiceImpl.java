package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.services.indexing.LemmaFinder;
import searchengine.services.search.SnippetParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    //        public Page<Book> getPageOfNewBooksDateBetween(Date from, Date to, Integer offset, Integer limit) {
//            return bookRepository.findAllByPubDateBetweenOrderByPubDateDesc(from, to, PageRequest.of(offset, limit));
//        }

    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lf;


    // TODO:
    //  - не получается получить правильно сниппеты соответсвующие контенту сайта
    //  - отсортировать и вернуть список сайтов
    //  - как возвращать Pageble<SearchData>
    public SearchResponse search(String query, Integer offset, Integer limit) {
        Set<String> lemmaSet = lf.getLemmaSet(query);
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInOrderByFrequencyAsc(lemmaSet);
        Set<PageEntity> pages = new HashSet<>();
        if (!lemmas.isEmpty()) {
            pages.addAll(lemmas.get(0).getPages());
            IntStream.range(1, lemmas.size()).takeWhile(i -> !pages.isEmpty()).forEach(i -> pages.retainAll(lemmas.get(i).getPages()));
            if (pages.isEmpty()) {
                return new SearchResponse(true, 0, new ArrayList<>());
            }
        }

        // TODO: сгенеирить обьект  можно пройти сделать ThreadExecutor для того
        List<SearchData> searchDataList = new ArrayList<>();
        for (PageEntity page : pages) {
            SiteEntity site = page.getSite();
            Document doc = Jsoup.parse(page.getContent());
            SnippetParser pageSnippet = new SnippetParser(doc, lf, lemmaSet);

            SearchData data = new SearchData();
            searchDataList.add(data);
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setTitle(doc.title());
            data.setSnippet(pageSnippet.getSnippet());

            //TODO: релевантность кол-во повторений получить для страницы с бд
            data.setRelevance(1.0F);
        }

        searchDataList.sort((o1, o2) -> (int) (o2.getRelevance() - o1.getRelevance() * 100));
        return new SearchResponse(true, searchDataList.size(), searchDataList);
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        return new SearchResponse();
    }
}

