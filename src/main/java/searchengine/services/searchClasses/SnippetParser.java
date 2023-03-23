package searchengine.services.searchClasses;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.services.indexingTasks.LemmaFinder;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// TODO: можно сделать бином или компонентом
public class SnippetParser {
    private final static Document.OutputSettings outputSettings = new Document.OutputSettings();
    private final static Map<String, Float> tag2Relevance;

    static {
        Map<String, Float> map = Stream.of(
                new AbstractMap.SimpleEntry<>("meta", 1F),
                new AbstractMap.SimpleEntry<>("title", 1F),
                new AbstractMap.SimpleEntry<>("h1", 0.8F),
                new AbstractMap.SimpleEntry<>("h2", 0.8F),
                new AbstractMap.SimpleEntry<>("h3", 0.8F),
                new AbstractMap.SimpleEntry<>("h4", 0.8F),
                new AbstractMap.SimpleEntry<>("h5", 0.8F),
                new AbstractMap.SimpleEntry<>("h6", 0.8F),
                new AbstractMap.SimpleEntry<>("p", 0.5F),
                new AbstractMap.SimpleEntry<>("ul", 0.5F),
                new AbstractMap.SimpleEntry<>("ol", 0.5F),
                new AbstractMap.SimpleEntry<>("dl", 0.5F),
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
    private final Map<String, SnippetDto> snippetsMap = new HashMap<>();
    private final Map<String, SnippetDtoSimple> snippetsMapSimple = new HashMap<>();
    private final Set<MapperElement2Match> snippetsSetDifficult = new TreeSet<>();
    private final Set<Snippet> snippetsSet = new TreeSet<>();
    private final String documentText;
    private final int maxSnippetLength = 300;
    private final int prefixSnippetLength = 50;

    public SnippetParser(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = lemmas;
        this.documentText = document.wholeText().replaceAll("\\n+\\s+", "\n");
        lemmas.forEach(i -> snippetsMap.put(i, new SnippetDto()));
        lemmas.forEach(i -> snippetsMapSimple.put(i, new SnippetDtoSimple()));
//        findSnippetsByElement(document);
        findSnippetsByAnotherWay(document);
        // findSnippetsSimple(document);  // простой вариант по всему документу
        // разбор по тэгам
    }

    /**
     * находит совпадения лемм по всему тексту без привязки к тэгам
     *
     * @param document
     */
    @Deprecated
    private void findSnippetsSimple(Document document) {
        String lowerText = document.wholeText().toLowerCase(Locale.ROOT);
        String[] words = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
        AtomicBoolean allSnippetsFound = new AtomicBoolean(false);
        for (String word : words) {
            lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                SnippetDtoSimple snippetDto = snippetsMapSimple.get(lemma);
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                while (m.find()) {
                    SnippetMatchSimple match = new SnippetMatchSimple(m.start(), m.end());
                    snippetDto.addMatch(match);
                }
                if (snippetsMapSimple.values().stream().noneMatch(SnippetDtoSimple::isMatchesEmpty)) {
                    allSnippetsFound.set(true);
                }
            });
            if (allSnippetsFound.get()) {
                return;
            }
        }
    }

    private void findSnippetsByAnotherWay(Document document) {
        String lowerText = documentText.toLowerCase(Locale.ROOT);
        String[] lowWords = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");

        // находим все леммы на странице записываем их в mathces
        Set<Match> matches = new TreeSet<>();
        Arrays.stream(lowWords).forEach(word -> {
            lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                matches.addAll(m.results()
                        .map(i -> new Match(i.start(), i.end(), lemma, documentText.substring(i.start(), i.end())))
                        .collect(Collectors.toSet()));
            });
        });

        // TODO: удобно сделать в классе Snippet и вернуть коллецию сниппетов
        // собираем из matches snippet
        Match[] matchesArray = matches.toArray(new Match[0]);
        IntStream.range(0, matches.size()).forEach(iArray -> {
            Snippet snippet = new Snippet();
            snippetsSet.add(snippet);

            int startSnippet = matchesArray[iArray].getStart();
            for (int iSnippet = 0; iArray + iSnippet < matches.size(); iSnippet++) {
                // TODO: удобнее do While цикл
                // первый сниппет полюбому надо добавить
                // слудующий если до него меньше чем до разрешенного сниппета
                Match m = matchesArray[iArray + iSnippet];
                // TODO: не совсем верно работает предыдущий snippet может попасть в зазор prefix
                if (m.getEnd() - startSnippet < maxSnippetLength - prefixSnippetLength * 2) {
                    snippet.addMatch(m);
                } else {
                    break;
                }
            }
        });
    }

    @Deprecated
    private void findSnippetsByElement(Document document) {
        Elements elements = document.getAllElements();
        int tagCount = 0;
        for (Element element : elements) {
            if (!element.tagName().equals("a")) {
                return;
            }

            String tag = element.tagName();
            Float tagRelevance = tag2Relevance.get(tag) != null ? tag2Relevance.get(tag) : 0F;
            MapperElement2Match snipppetMatcher = new MapperElement2Match(tagCount, tagRelevance, element);

            String lowerText = document.text().toLowerCase(Locale.ROOT);
            // String[] sentences = lowerText.split("[?!.](\\s+|$)");
            String[] lowWords = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");
            Arrays.stream(lowWords).forEach(word -> {
                lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                    Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);

                    // определять с большой ли оно буквыы
                    m.results().forEach(result -> snipppetMatcher.addMatch(new Match(result.start(), result.end(), lemma)));
                });
            });

            if (!snipppetMatcher.getMatchesList().isEmpty()) {
                snippetsSetDifficult.add(snipppetMatcher);
            }
            tagCount++;
        }

        // TODO здесь можно заменить весь текст с <b></b> и добавить в  SnippetMatchDifficult snipppetMatcher = new SnippetMatchDifficult(tagCount, tagRelevance, element
        // можно сэйвить основной текст и каждый раз вызывать m.replaceall на word с <b>
        // сэйвить этот text в snippetsSetDifficult
        // но тогда нужно и нарезать текст а матчеры, потому что не будет информации о start end как уместить текст в заданное кол-во символов
    }


    public String getSnippet2() {
        // TODO: для произволительности это делать в getSnippet только для нужных сниппетов
        // взять префикс и постфикс и отделить до конца начала предложения или пробела
        // с большой ли буквы первый match
        // заменить \n на как в гугле " * ", если не дошли до \n поставить ...
        // вставить <b></b>
        // докинуть не найдено в getSnippet
        // replace через здешний StringBuilder

        // TODO: надо двигать и если уже нашел сниппет с леммами убрать их и следующий искать с другими
        // собирать куски сниппетов в join и join их по посднуму элементу \n или ...
        // и потом можно зареплейсить \n
        // TODO: собирать в ListSnippet и делать setSnippet в стриме
        // в конец докинуть lemma которые не найдены
        // TODO: скинуть в методы функции getPsotfix и getPrefixs
        // TODO: рассчитать длинну на лемму и исходя из этого подбирать длинну сниппета
        // это также зависит от функции find, т.к. сниппет там собиарется

        // TODO: здесь не forEAch
        StringBuilder builder = new StringBuilder();
        snippetsSet.stream().takeWhile(i -> !lemmas.isEmpty())
                .filter(i -> i.getLemmaSet().stream().anyMatch(lemmas::contains)).map(snippet -> {
            lemmas.removeAll(snippet.getLemmaSet());
            String string = getSnippetPartSting(snippet);
            // TODO: или на знаки законченного предложения
            if (!string.endsWith("\n")) {
                string = string.concat("... ");
            }
            snippet.setSnippet(string);
            return string;
        }).forEach(builder::append);

        if (!lemmas.isEmpty()) {
            builder.append("<br/>Не найдено: <s>");
            builder.append(String.join("</s>, <s>", lemmas));
            builder.append(".");
        }
        String snippet = builder.toString();
        return snippet.replaceAll("\n", " * ");
    }

    // TODO: нужно разделить по \s и добавить .. если не найден разделитель
    private String getPrefix(Match match) {
        String[] prefixSplit = (match.start - prefixSnippetLength > -1 ?
                documentText.substring(match.start - prefixSnippetLength, match.start) :
                documentText.substring(0, match.start)).split("[\n?!.]+");  // "[?!.](\\s+|$)"
        return prefixSplit[prefixSplit.length - 1];
    }

    private String getPostfix(Match match) {
        String[] postfixSplit = (match.end + prefixSnippetLength < documentText.length() - 1 ?
                documentText.substring(match.end, match.end + prefixSnippetLength) :
                documentText.substring(match.end)).split("[\n?!.]+");   // "[?!.](\\s+|$)"
        return postfixSplit[0];
    }

    private String getSnippetPartSting(Snippet snippet) {
        // TODO: можно возвращать boolean и делать setSnippet
        TreeSet<Match> matches = snippet.getMatchesSet();
        if (matches.isEmpty()) {
            return "";
        }
        StringBuilder string = new StringBuilder();
        Match first = matches.first();
        Match last = matches.last();
        if (first.equals(last)) {
            string.append(getPrefix(first));
            string.append("<b>");
            string.append(documentText, first.start, first.end);
            string.append("</b>");
            string.append(getPostfix(first));
            return string.toString();
        }
        // TODO: здесь наверное циклом т.к. надо проверить какие леммы нужны в сниппете хотя можно поставить takewhile
        Match[] matchesArray = matches.toArray(new Match[0]);
        IntStream.range(0, matchesArray.length).forEach(i -> {
            Match match = matchesArray[0];
            if (match.equals(first)) {
                string.append(getPrefix(match));
                string.append("<b>");
                string.append(documentText, match.start, match.end);
                string.append("</b>");
            } else if (match.equals(last)) {
                // TODO: от предыдущено энд до нового энд
                Match prevMatch = matchesArray[i - 1];
                string.append(documentText, prevMatch.end, match.start);
                string.append("<b>");
                string.append(documentText, match.start, match.end);
                string.append("</b>");
                string.append(getPostfix(match));
            } else {
                Match prevMatch = matchesArray[i - 1];
                string.append(documentText, prevMatch.end, match.start);
                string.append("<b>");
                string.append(documentText, match.start, match.end);
                string.append("</b>");
            }
        });
        return string.toString();
    }

    /**
     * находит слово, выбирает предложение, но при этом стирает знаки перепинания предложения
     * обьединяет предложения в сниппет
     *
     * @return
     */
    @Deprecated
    public String getSnippetSimple() {
        StringBuilder buffer = new StringBuilder();
        List<String> joins = new ArrayList<>();
        String text = document.wholeText();

        TreeSet<SnippetMatchSimple> matches = new TreeSet<>();
        snippetsMapSimple.values().forEach(x -> matches.add(x.getMatchers().first()));  // добавем в matches первый или последний обьек
        int range = 100;
        for (SnippetMatchSimple s : matches) {
            int wordStart = s.getStart();
            int wordEnd = s.getEnd();
            String[] prefix = (wordStart - range > -1 ?
                    text.substring(wordStart - range, wordStart) :
                    text.substring(0, wordStart)).split("[\n?!.]+");
            String[] postfix = (wordEnd + range < text.length() - 1 ?
                    text.substring(wordEnd, wordEnd + range) :
                    text.substring(wordEnd)).split("[\n?!.]+");

            buffer.append(prefix[prefix.length - 1]);
            buffer.append("<b>");
            buffer.append(text, wordStart, wordEnd);
            buffer.append("</b>");
            buffer.append(postfix[0]);

            joins.add(buffer.toString());
            buffer.setLength(0);
        }
        return String.join("...", joins);
    }


    /**
     * не доделано, не ясно как взять сниппет идя по match
     * сложно обьеденять сниппет по тэгам
     * нужен буфер сниппета
     *
     * @return
     */
    @Deprecated
    public String getSnippetByElement() {
        StringBuilder snippetPart = new StringBuilder();
        HashSet<String> lemmas = new HashSet<>(this.lemmas);    // необязательно копировать обьект
        int symbolsForSnippet = 300;
        int symbolsByLemma = symbolsForSnippet / lemmas.size();
        Pattern p = Pattern.compile("[?!.](\\s+|$)", Pattern.CASE_INSENSITIVE);

        // TODO: получить список по словам, потом sjoinint в builder
        List<String> snips = snippetsSetDifficult.stream().takeWhile(i -> !lemmas.isEmpty()).map(i -> {
            Set<String> elementLemmas = i.getLemmaSet();  // или getMatchesMap().keySet()
            Set<String> snippetLemmas = lemmas.stream().filter(elementLemmas::contains).collect(Collectors.toSet());
            if (snippetLemmas.isEmpty()) {
                return null;
            }
            lemmas.removeAll(snippetLemmas);
            String text = i.getElement().ownText();

            // TODO: последний индекс первого совпадения использовать когда не вмещается в snippet
            List<Integer> endIndexes = i.getMatchesMap().values().stream().map(iw -> iw.get(0).getEnd()).collect(Collectors.toList());

            // TODO: определяем предложения
            Matcher m = p.matcher(text);
            List<Integer> senteces = m.results().map(MatchResult::end).collect(Collectors.toList());

            int startIndex = 0;
            int endIndex = 0;
            int halfSymbolsSnippetByLemma = symbolsByLemma / 2;
            for (Match w : i.getMatchesList()) {

                // TODO: ограничить следующим индексом в стриме нельзя взять следущий или Exception из-за w.start
                //  когда startIdex > w.start
                String prefix = w.start - halfSymbolsSnippetByLemma > -1 ?
                        text.substring(w.start - halfSymbolsSnippetByLemma, w.start) : text.substring(startIndex, w.start);
                String postfix = w.end + halfSymbolsSnippetByLemma < text.length() - 1 ?
                        text.substring(w.end, w.end + halfSymbolsSnippetByLemma) : text.substring(w.end);
                String word = text.substring(w.start, w.end);

                // TODO: кидать в обьект с matcher слова, а потом просто зареплейсить их в тексте
                startIndex = w.start + postfix.length();
                snippetLemmas.remove(w.getLemma());
                snippetPart.append(prefix).append("<b>").append(word).append("</b>").append(postfix);
            }

            String snippetElement = snippetPart.toString();
            snippetPart.setLength(0);
            return snippetElement;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return String.join("<br/>", snips);

        // TODO: буфер из regex repalceALl
// берем первый сабстринг от 0 до старт в , обрезаем по начало предложения
//                public String replaceAll(String replacement) {
//                    reset();
//                    boolean result = find();
//                    if (result) {
//                        StringBuilder sb = new StringBuilder();
//                        do {
//                            appendReplacement(sb, replacement);
//                            result = find();
//                        } while (result);
//                        appendTail(sb);
//                        return sb.toString();
//                    }
//                    return text.toString();
//                }


//         Optional<Map.Entry<String, SnippetDto>> longestEntry = snippetsMap.entrySet()
//         .stream().filter(i -> !i.getValue().isMatchesEmpty())
//         .max(Comparator.comparingInt(x -> x.getValue().getMatchers().first().getStart()));
//         if (longestEntry.isEmpty()) {
//         return "Не нашли <b>сниппет</b>";
//         }
//         TreeSet<SnippetMatch> matches = new TreeSet<>();
//         SnippetMatch longestMatch = longestEntry.get().getValue().getMatchers().first();
//         matches.add(longestMatch);
//         //        List<Optional<SnippetMatch>> collect = snippetsMap.values().stream().filter(i -> !i.getMatchers().equals(s.get().getValue().getMatchers()))
//         //                .map(x -> x.getMatchers().stream().min(Comparator.comparingInt(i -> (i.getStart() - longestMatch.getStart()))))
//         //                .collect(Collectors.toList());
//         for (SnippetDto s : snippetsMap.values()) {
//         if (s.equals(longestEntry.get().getValue())) {
//         continue;
//         }
//         // TODO: можно закинуть в список ifPresentOrElse если не найдет совпадений что не найдено
//         s.getMatchers().stream().min(Comparator.comparingInt(i -> (i.getStart() - longestMatch.getStart())))
//         .ifPresent(matches::add);
//         }
        // TODO: сконнектить сниппеть
        //return "Не нашли <b>сниппет</b>";
    }


}
