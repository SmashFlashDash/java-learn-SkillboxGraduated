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
//        Document.OutputSettings outputSettings = new Document.OutputSettings();
//        outputSettings.prettyPrint(false);
        List<SearchData> searchDataList = new ArrayList<>();
        for (PageEntity page : pages) {
            SiteEntity site = page.getSite();
            Document doc = Jsoup.parse(page.getContent());
            PageSnippet pageSnippet = new PageSnippet(doc, lf, lemmaSet);

            // TODO: сделать обьект в который будет происходить парсинг текста
            //  циклом идти по тэгам, получать текст тэгов и получать релевантность
            //  если все найдены выйти из цикла
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
    private final Document document;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Map<String, SnippetDto> snippets;
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

    // TODO: можно искать по селектору тэгу сниппеты лемм
    //  если ищем по тэгу нужно сразу брать сниппет как нашли
    //  затем убираем лемму из поиска, или при повторном нахождении сравниваем релевантность и можем сохранить старую релевантноссть
    //  но  засейвить новый сниппет
    //  как только все сниппеты найдены прекратить поиск
    //  - если ищем по всему тексту сразу также находим сниппеты и закидываем, например можно получить все элементы и тогда будет зависимость релевантности
    //  - стоит найти совпадения сначала в одних тэгах или самые ближние друг к другу
    //  тоесть сразу взять весь текст и идти по нему
    //  если берем весь текст html без документа не сможем определить тэг но поиск буде быстрее
    //  findSnippets(Jsoup.clean(document.body().html(), "", Safelist.simpleText(), outputSettings));
    //  - весь текст по html
    //  - все элементы html и текст по каждому
    //  - выбрать элементы которые ищем и идти по каждому
    //  - выбрать сначала один тип тэга потом другой и т.д.
    //  самое оптимальное наверное получить все элекменты документа и брать по ним текст
    //  -
    //  - самое оптимальное при парсинге старниц, записывать кол-во лемм на странице и определать наибольшей тэг и плюсовать его в таблицу
    //  - потом когда ищем поисковый запрос преобразуем его в леммы, и ищем страницы с такими леммаи
    //  - потом ищем сниппеты
    //  -
    //  //  поиск регуляркой по сниппетам
    //  //  (([\\n.?!;]|...)(word1|word2)([\\n.?!;]|...))
    //  // если найдет в одном и том же предложении можно сравнить
    //  тогда когда ищем сниппет берем только слово
    public PageSnippet(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = lemmas;
        this.snippets = lemmas.stream().collect(Collectors.toMap(i -> i, i -> new SnippetDto()));

        Elements elements = document.getAllElements();
        for (Element element : elements) {
            String tag = element.tagName();
            String text = element.text();
            if (!text.isBlank()) {
                //findSnippets(Jsoup.clean(element.html(), "", Safelist.simpleText(), outputSettings), tag);
                findSnippets(text, tag);
            }
        }
//        for (String tag : tag2Relevance.keySet()) {
//            Elements elements = document.select(tag);
//        }
    }

    // TODO: в обьекте this.snippets записываем в snippet и relevance by tag
    //  snippet надо брать в три строки
    //  можео взять регуляркой по этому слову по предложению в +- по кол-ву символов не менее
    //  либо взять substring по text
    //  либо взять массивом по словам, но там не все знаки препинаня
    // разделить на предложения .split("[\\n!.?]"); // |<[^>]*>]")

    // TODO: или сначала сплитануть текст на предложения а потом на слова
    //  и в сниппет кидать предложение которое идет и предыдущее и следующее
    //  а если они не последовательны то соединять ... и не к началу предложения а за несколько слов
    //  -
    //  или записать все матчеры, а парсить уже весь текст когда получаем сниппет

    // TODO: взять на 100 символов в стороны, разделить текст на предложения
    // записать префикс и постфикс сниппета и самов слово
    private void findSnippets(String text, String tag) {
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
        for (String word : words) {
            String lemma = lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().orElse(null);
            if (lemma != null) {
                SnippetDto snippet = snippets.get(lemma);
                // snippet.setSnippet(" wow ");
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(text);
                if (m.find()) {

                    int wordStart;
                    int wordEnd;
                    wordStart = m.start();
                    wordEnd = m.end();
                    snippet.setPrefix(wordStart - 100 > -1 ? text.substring(wordStart - 100, wordStart) : text.substring(0, wordStart));
                    snippet.setPostfix( wordEnd + 100 < text.length() - 1 ? text.substring(wordEnd, wordEnd + 100) : text.substring(wordEnd));
                    snippet.setMidfix(text.substring(wordStart, wordEnd));
                    snippet.setStart(wordStart);
                    snippet.setEnd(wordEnd);
                    snippet.addMatch(new Integer[]{wordStart, wordEnd});
                }
                // Matcher m1 = Pattern.compile(String.format("(([.?!;]|...).+(%s).+([.?!;]|...))", word), Pattern.CASE_INSENSITIVE).matcher(text);
                // m1.find();

                // TODO: или убрать из списка lem но так найдем ближайший к остальныи сниппетам
                Float tagRelevance = tag2Relevance.get(tag);
                if (tagRelevance != null) {
                    Float relevance =  snippet.getRelevance();
                    if (relevance == null) {
                        snippet.setRelevance(tagRelevance);
                    } else if (relevance < tagRelevance) {
                        snippet.setRelevance(tagRelevance);
                    }
                }
                // все сниппеты найдены
                if (snippets.values().stream().noneMatch(i -> i.getSnippet().isBlank())) {
                    break;
                }
            }
        }
    }

    public String getSnippet() {
        // TODO: выстроить ближайшие сниппеты по matches
        for (Map.Entry<String,SnippetDto> s : snippets.entrySet()){
            s.getValue().getMatchers();
        }
        return "";
    }

    private String findSnippets(String text) {
        // вариант поиска по тегам для определения релевантности по тегу

        StringBuilder snippet = new StringBuilder();
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
        int index;
        String substring;
        for (String word : words) {
//            // TODO: найти слово по форме
//            //  на странице может быть несколько совпадений
////            if (lf.getLemmaSet(word).contains(lemma)) {

            // TODO: найти слово по первой лемме, и далее по всем, получить их индексы
            //  можно указать до слова любые знаки кро
            //  леммы могут быть в другом порядке поэтому нужен Map или лист отсортированный по index start
            //  если уже была добалвена значение должно перезаписаться только если ближе к остальныи по index start
            //  если ншел по всем лемам можно остановить поиск
            //  - можно делать поиск по элемента h1 потом h2 meta=description
            //  - в зависимости от тэга установить relevance обьявить ее списком или map
            //  - при этом иметь один обьект по поиску из которого сформируем snippet
            String lemma = lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().orElse(null);
            if (lemma != null) {        //   lf.isLemmaApplyWord(lemma, word)) {
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(text);
                if (m.find()) {
                    index = m.start();
                    substring = index - 100 > -1 ? text.substring(index - 100, index) : text.substring(0, index);
                    String[] startSnipet = substring.split("[\\n!.?]"); // |<[^>]*>]")

                    index = m.end();
                    substring = index + 100 < text.length() - 1 ? text.substring(index, index + 100) : text.substring(index);
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

@Data
@NoArgsConstructor
@AllArgsConstructor
class SnippetDto {
    Integer start;
    Integer end;
    Float relevance;
    String snippet;
    String prefix;
    String midfix;
    String postfix;
    List<Integer[]> matchers = new ArrayList<>();

    public void addMatch(Integer[] match) {
        matchers.add(match);
    }

    public List<Integer[]> getMatchers(){
        return matchers;
    }
}
