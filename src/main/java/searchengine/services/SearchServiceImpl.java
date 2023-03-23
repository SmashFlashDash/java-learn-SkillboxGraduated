package searchengine.services;

import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.services.indexingTasks.LemmaFinder;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;
import searchengine.services.searchClasses.SnippetParser;

import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    //        public Page<Book> getPageOfNewBooksDateBetween(Date from, Date to, Integer offset, Integer limit) {
//            return bookRepository.findAllByPubDateBetweenOrderByPubDateDesc(from, to, PageRequest.of(offset, limit));
//        }

    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lf;

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
        // TODO: если в lemmas нет entity значит не найдена вообще
        // if (!lemmas.isEmpty()) {
        //     pages.addAll(lemmas.get(0).getPages());
        //     for (int i = 1; i < lemmas.size(); i ++) {
        //         List<PageEntity> nextPages = lemmas.get(i).getPages();
        //         pages.retainAll(nextPages);
        //         if (pages.isEmpty()) {
        //             // TODO: можно сразу делать return
        //             break;
        //         }
        //     }
        // }

        // TODO: сгенеирить обьект  можно пройти forkJoin
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
            //data.setSnippet(pageSnippet.getSnippetByElement());
            data.setSnippet(pageSnippet.getSnippet2());
            data.setRelevance(1.0F);
        }
        return new SearchResponse(true, searchDataList.size(), searchDataList);
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        return new SearchResponse();
    }
}


//------------------Виды алгшоритмов------------------
// TODO: правильный алгоритм расположить match, по тэгу
// те которые содержаться в одном тэге складывать в месте
// сортировать тэги по релевантности
// допустим если в одном тэге совпадения на нексколько лемм
// то можно класть список match
// с обьектами в которых храниться сам тэг и совпадения по нему
// TODO: сделать обьект в котором есть тэг, релевантность, список присутствующих лемм, все совпадения с start end
//  сортировать тэги по кол-ву присутствующих лемм
//  если нужны тэги с другими лемма найти самую релевантную
//------------------Виды алгшоритмов------------------

// с сложным тэгами мы у нас могут содеражаться в одном тэге, сплитуем предложения по первому mathc
// берем substring предложение например на 100 символов, split их
// для превикса берем первый, для постфикса второй, слово оборачиваем в <b>
// можем пройти по лемма и найти повторки этого тэга и обернуть слово
// если их нет берем следующий матч ближайшего слова или по релевантности


