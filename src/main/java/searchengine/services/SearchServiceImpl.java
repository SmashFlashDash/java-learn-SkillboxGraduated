package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.dto.indexingTasks.LemmaFinder;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.LemmaRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    //        public Page<Book> getPageOfNewBooksDateBetween(Date from, Date to, Integer offset, Integer limit) {
//            return bookRepository.findAllByPubDateBetweenOrderByPubDateDesc(from, to, PageRequest.of(offset, limit));
//        }

    private final LemmaRepository lemmaRepository;
    private final LemmaFinder lf;

    public SearchResponse search(String query, Integer offset, Integer limit){
        // TODO:
        //  разбить на слова преобразовать в леммы
        //  искать совпадения лемм в БД
        Set<String> lemmaSet = lf.getLemmaSet(query);
        // TODO: может здесь не нужно получать Page а Page делать из расчета страниц?
        //  - в запросе надо исключать часто встречаемые леммы определить в процентах может быть от самой встречаемой
        //  может зависить от кол-ва сайто или кол-ва страниц
        // Page<LemmaEntity> page = lemmaRepository.findAllByLemmaInOrderByFrequencyAsc(lemmaSet, PageRequest.of(offset, limit));
        // TODO: получить страницы потом отбросить в которых нет нужной
        // List<LemmaEntity> ll = page.getContent();
        // List<PageEntity> pages = ll.get(0).getPages();
        // TODO: можн set ratainAll union intrsection
        // Set<PageEntity> set = new HashSet<>();

        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInOrderByFrequencyAsc(lemmaSet);
        int i = 0;
        // TODO: если указан сайт нужно страници по сайту отфильтрвоать
        List<PageEntity> pages = lemmas.get(i++).getPages();
        while (pages.size() > 0) {
            List<PageEntity> nextPages = lemmas.get(i++).getPages();
            pages = pages.stream().filter(nextPages::contains).collect(Collectors.toList());
        }


        // TODO: сгенеирить обьект  можно пройти forkJoin
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        List<SearchData> searchDataList = new ArrayList<>();
        for (PageEntity page : pages) {
            Document doc = Jsoup.parse(page.getContent());
//            String htmlText = Jsoup.clean(doc.select("h1,h2,h3,h4,h5,h6,p,meta,ul,ol,dl,span").html(), "", Safelist.simpleText(), outputSettings);
            String htmlText = Jsoup.clean(doc.body().html(), "", Safelist.simpleText(), outputSettings);
            SiteEntity site = page.getSite();
            String snippet = getSnippet(htmlText, lemmaSet);
            SearchData data = new SearchData();
            searchDataList.add(data);
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setTitle(doc.title());
            data.setSnippet(snippet);
            data.setRelevance(1.0F);
        }
        return new SearchResponse(true, searchDataList.size(), searchDataList);
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        return new SearchResponse();
    }

//    private List<SearchData> toSearchData(LemmaEntity lemma) {
//        Document.OutputSettings outputSettings = new Document.OutputSettings();
//        outputSettings.prettyPrint(false);
//
//        SiteEntity site = lemma.getSite();
//        List<PageEntity> pages = lemma.getPages();
//        List<SearchData> searchesData = new ArrayList<>();
//        for (PageEntity page : pages) {
//            // TODO: парсинг сниппета
//            //  можно сделать обьект snippeta а не в функции парсить
//            //  еще можно делать select по тэгам чтобы найти в заголвоке в потом в параграфах
//            //  и находить первый самое совпадение по семантике
//            Document doc = Jsoup.parse(page.getContent());
//            //String htmlText = doc.select("h1,h2,h3,h4,h5,h6,p").html();
//            String htmlText = Jsoup.clean(doc.body().html(), "", Safelist.simpleText(), outputSettings);
//            String snippet = getSnippet(htmlText, lemma.getLemma());
//            if (snippet.isEmpty()) {
//                continue;
//            }
//
//            SearchData data = new SearchData();
//            searchesData.add(data);
//            data.setSite(site.getUrl());
//            data.setSiteName(site.getName());
//            data.setTitle(doc.title());
//            data.setSnippet(snippet);
//            data.setRelevance(1.0F);        // TODO: раасчитать релевантность
//        }
//        return searchesData;
//    }

    private String getSnippet(String text, Set<String> lemmas){
        // TODO: идти по тексту и искать совпадения с lemma
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
        StringBuilder snippet = new StringBuilder();
        int index;
        String substring;
        for (String word : words) {
//            // TODO: найти слово по форме
//            //  на странице может быть несколько совпадений
////            if (lf.getLemmaSet(word).contains(lemma)) {
            if (lemmas.stream().anyMatch(l -> lf.isLemmaApplyWord(l, word))) { //   lf.isLemmaApplyWord(lemma, word)) {
               Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(text);
               if (m.find()) {
                   //
                   index = m.start();
                   substring = index - 100 > -1 ? text.substring(index-100, index) : text.substring(0, index);
                   String[] startSnipet = substring.split("[\\n!.?]"); // |<[^>]*>]")

                   index = m.end();
                   substring = index + 100 < text.length() - 1 ? text.substring(index, index+100) : text.substring(index);
                   String[] endSnipet = substring.split("[\\n!.?]"); // |<[^>]*>");

                   // TODO: что если массив будет пустой то надо подставить ""
                   //  взять несколько предложений пока не наберется кол-во слов
                   snippet.append(startSnipet[startSnipet.length - 1].trim())
                           .append(" <b>").append(m.group()).append("</b> ")
                           .append(endSnipet[startSnipet.length - 1].trim());
                   return snippet.toString();
               }
            }
        }
        return "";
    }
}
