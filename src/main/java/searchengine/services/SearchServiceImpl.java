package searchengine.services;

import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
            PageSnippet pageSnippet = new PageSnippet(doc, lf, lemmaSet);

            SearchData data = new SearchData();
            searchDataList.add(data);
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setTitle(doc.title());
            data.setSnippet(pageSnippet.getSnippet());
            data.setRelevance(1.0F);
        }
        return new SearchResponse(true, searchDataList.size(), searchDataList);
    }

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        return new SearchResponse();
    }
}

// TODO: можно сделать бином или компонентом
class PageSnippet {
    private final static Document.OutputSettings outputSettings = new Document.OutputSettings();
    private final static Map<String, Float> tag2Relevance;

    static {
        Map<String, Float> map = Stream.of(
                new AbstractMap.SimpleEntry<>("h1", 1F),
                new AbstractMap.SimpleEntry<>("h2", 0.8F),
                new AbstractMap.SimpleEntry<>("h3", 0.8F),
                new AbstractMap.SimpleEntry<>("h4", 0.8F),
                new AbstractMap.SimpleEntry<>("h5", 0.6F),
                new AbstractMap.SimpleEntry<>("h6", 0.6F),
                new AbstractMap.SimpleEntry<>("p", 0.5F),
                new AbstractMap.SimpleEntry<>("meta", 1F),
                new AbstractMap.SimpleEntry<>("ul", 0.8F),
                new AbstractMap.SimpleEntry<>("ol", 0.8F),
                new AbstractMap.SimpleEntry<>("dl", 0.8F),
                new AbstractMap.SimpleEntry<>("div", 0.5F),
                new AbstractMap.SimpleEntry<>("span", 0.5F))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        tag2Relevance = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        outputSettings.prettyPrint(false);
    }

    private final Document document;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Map<String, SnippetDto> snippetsMap;

    public PageSnippet(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = lemmas;
        this.snippetsMap = lemmas.stream().collect(Collectors.toMap(i -> i, i -> new SnippetDto()));

        Elements elements = document.getAllElements();
        for (Element element : elements) {
            String tag = element.tagName();
            String text = element.text();
            if (!text.isBlank()) {
                //findSnippets(Jsoup.clean(element.html(), "", Safelist.simpleText(), outputSettings), tag);
                findSnippets(text, tag);
            }
        }
    }

    // TODO: использовать HashMap<String, SnippetdDto>
    //  с полями maxTagRelevace, List<Iteger[]>
    //  в гет сниппет, найдем первый с минимальной длинной
    //  в остпльнрых ищем ближайшие к нему по старт
    //  берем по +100 -100 символов от них
    private void findSnippets(String text, String tag) {
        final String lowerText = text.toLowerCase(Locale.ROOT);
        Float tagRelevance = tag2Relevance.get(tag) != null ? tag2Relevance.get(tag) : 0F;

        String[] words = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
        for (String word : words) {
            lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                SnippetDto snippetDto = snippetsMap.get(lemma);
                Float relevance = snippetDto.getMaxTagRelevance();
                if (relevance == null || relevance < tagRelevance) {
                    snippetDto.setMaxTagRelevance(tagRelevance);
                }
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                while (m.find()) {
                    SnippetMatch match = new SnippetMatch(m.start(), m.end(), tagRelevance);
                    snippetDto.addMatch(match);
                }
                // TODO: флаг котоырй ставить функцией найдены ли все сниппеты, и в цикле выходит из функции
            });
//            Optional<String> lemma = lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst();
//            if (lemma.isPresent()) {
//                SnippetDto snippetDto = snippetsMap.get(lemma);
//                Float relevance = snippetDto.getMaxTagRelevance();
//                if (relevance == null || relevance < tagRelevance) {
//                    snippetDto.setMaxTagRelevance(tagRelevance);
//                }
//                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(text);
//                while (m.find()) {
//                    SnippetMatch match = new SnippetMatch(m.start(), m.end());
//                    snippetDto.addMatch(match);
//                }
//                // Matcher m1 = Pattern.compile(String.format("(([.?!;]|...).+(%s).+([.?!;]|...))", word), Pattern.CASE_INSENSITIVE).matcher(text);
//                // m1.find();
//                // все сниппеты найдены
//                if (snippetsMap.values().stream().noneMatch(SnippetDto::isMatchesEmpty)) {
//                    return;
//                }
//            }
        }
    }

    public String getSnippet() {
        // TODO:
        //  сначала найдем самый больший по старт первый элемент
        //  потом найдем в оставшихся lemma ближайшие к нему
        //  считаем расстояние по тексту
        //  прасим substring или регуляркой делим на предложения
        //  - можно сортирнуть по релевантности тэга
        //  кинуть только нужные тэги в jsoup.select

        // TODO: можно те которые в одном тэге выводить через ... если не хватает расстояния
        //  те которые в другом тэге выводить по максимально релевантности
        //  задаться кол-вом символов на весь сниппет
        //  разделить сколько-кол-во символов на тэг, не это надо определять когда уже нашли совпадения в одном тэге
        //  поэтому можно передать jsoup element в SnippetMatch

        Optional<Map.Entry<String, SnippetDto>> longestEntry = snippetsMap.entrySet()
                .stream().filter(i -> !i.getValue().isMatchesEmpty())
                .max(Comparator.comparingInt(x -> x.getValue().getMatchers().first().getStart()));
        if (longestEntry.isEmpty()) {
            return "Не нашли <b>сниппет</b>";
        }

        //snippetsMap.remove(s.get().getKey());
        TreeSet<SnippetMatch> matches = new TreeSet<>();
        SnippetMatch longestMatch = longestEntry.get().getValue().getMatchers().first();
        matches.add(longestMatch);
        // TODO: пройти по map найти ближайш по snippet
//        List<Optional<SnippetMatch>> collect = snippetsMap.values().stream().filter(i -> !i.getMatchers().equals(s.get().getValue().getMatchers()))
//                .map(x -> x.getMatchers().stream().min(Comparator.comparingInt(i -> (i.getStart() - longestMatch.getStart()))))
//                .collect(Collectors.toList());
        for (SnippetDto s : snippetsMap.values()) {
            // хотим найти ближайших
            if (s.equals(longestEntry.get().getValue())) {
                continue;
            }
            // TODO: можно закинуть в список ifPresentOrElse если не найдет совпадений что не найдено
            //  добавить в конце сниппета
            //  в тэгах брать meta и атрибут description
            s.getMatchers().stream().min(Comparator.comparingInt(i -> (i.getStart() - longestMatch.getStart())))
                    .ifPresent(matches::add);
        }
        // TODO: сконнектить сниппеть

        return "Не нашли <b>сниппет</b>";


//      snippet.setPrefix(wordStart - 100 > -1 ? text.substring(wordStart - 100, wordStart) : text.substring(0, wordStart));
//      snippet.setPostfix( wordEnd + 100 < text.length() - 1 ? text.substring(wordEnd, wordEnd + 100) : text.substring(wordEnd));
//      snippet.setMidfix(text.substring(wordStart, wordEnd));
//      snippet.addMatch(new Integer[]{wordStart, wordEnd});
    }

    //    private boolean isWordCloser(String lemma, int start) {
//        Integer prevStart = lemmasSnippet.get(lemma).getStart();
//        if (prevStart != null) {
//            Optional<SnippetDto> s = lemmasSnippet.entrySet().stream()
//                    .filter(i -> !i.getKey().equals(lemma) && i.getValue().getStart() != null)
//                    .map(Map.Entry::getValue)
//                    .min(Comparator.comparingInt(x -> Math.abs(x.getStart() - prevStart)));
////            if (s.isPresent()) {
////                s.get().
////            }
//
//        }
//        return true;
//    }
}

@NoArgsConstructor
class SnippetDto {
    @Getter
    private final TreeSet<SnippetMatch> matchers = new TreeSet<>();
    @Getter
    @Setter
    private Float maxTagRelevance = null;

    public void addMatch(SnippetMatch match) {
        matchers.add(match);
    }

    public boolean isMatchesEmpty() {
        return matchers.isEmpty();
    }
}

@Getter
@AllArgsConstructor
class SnippetMatch implements Comparable<SnippetMatch> {
    int start;
    int end;
    Float tagRelevance;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnippetMatch that = (SnippetMatch) o;
        return start == that.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start);
    }

    @Override
    public int compareTo(SnippetMatch o) {
        return Integer.compare(start, o.start);
    }
}


