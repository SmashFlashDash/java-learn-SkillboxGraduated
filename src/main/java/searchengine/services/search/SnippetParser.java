package searchengine.services.search;

import lombok.Getter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.LemmaFinder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SnippetParser {
    private final static Map<String, Float> tag2Relevance;

    static {
        Map<String, Float> map = new HashMap<>();
        map.put("meta", 10F);
        map.put("title", 1F);
        map.put("h1", 0.8F);
        map.put("h2", 0.8F);
        map.put("h3", 0.8F);
        map.put("h4", 0.8F);
        map.put("h5", 0.8F);
        map.put("h6", 0.8F);
        map.put("p", 0.5F);
        map.put("ol", 0.5F);
        map.put("ul", 0.5F);
        map.put("div", 0.5F);
        map.put("span", 0.5F);
        tag2Relevance = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private final Document document;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Set<Snippet> snippetsSet = new TreeSet<>();
    private final String documentText;
    private final int maxSnippetLength = 300;
    private final int prefixSnippetLength = 200;
    @Getter
    private Float snippetRelevance = 0F;

    public SnippetParser(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = new HashSet<>(lemmas);

        // this.documentText = String.join("\n", document.getAllElements()
        //         .stream().map(Element::ownText).collect(Collectors.toCollection(LinkedHashSet::new)))
        //         .replaceAll("\\n+\\s+", "\n");


        // поиск по тексту документа часто совпадает с меню сайта а не основным контентом
        this.documentText = document.body().wholeText().replaceAll("\\n+\\s+", "\n");
        findSnippetsByDocumentText();


        // по выбранным тэгам, и с определением релевантности по тэгам
        // this.documentText = document.wholeText().replaceAll("\\n+\\s+", "\n");
        // findSnippetsByElement(document);
    }

    /**
     * поиск сниппета по всему тексту документа
     */
    private void findSnippetsByDocumentText() {
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

        // собираем из matches snippet
        snippetsSet.addAll(mathesToSnippets(matches));
    }

    /**
     * поиск сниппета по элемента jsoup, определяет релевантность сниппета, в каком тэге найдены совпадения
     * @param document
     */
    @Deprecated
    private void findSnippetsByElement(Document document) {
        // TODO: нужно обьеденить сниппеты, или по другому брать тэги
        //  на некоторыех сайтах весь текст в div
        //  например jsoup select тэги которые содержат текст
        //  предварительно найти все слова по документу которые искать в тэгах
        Elements elements = document.select("meta[description],h1,h2,h3,h4,h5,h6,p,ol,ul");
        for (Element element : elements) {
            if (tag2Relevance.containsKey(element.tagName())) {
                snippetRelevance += tag2Relevance.get(element.tagName());
            }
            String text = element.wholeText().replaceAll("\\n+\\s+", "\n");
            int offset = documentText.indexOf(text);
            if (offset == -1) {
                continue;
            }
            String lowerText = text.toLowerCase(Locale.ROOT);
            String[] lowWords = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");

            Set<Match> matches = new TreeSet<>();
            Arrays.stream(lowWords).forEach(word -> {
                lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                    Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                    matches.addAll(m.results().map(i -> {
                        int start = i.start() + offset;
                        int end = i.end() + offset;
                        return new Match(start, end, lemma, documentText.substring(start, end));
                    }).collect(Collectors.toSet()));
                });
            });
            // сниппет из совпадений по элементу
            snippetsSet.addAll(mathesToSnippets(matches));
        }
        // TODO: пройти по сделанным сниппетам и
        // и их в новые сниппеты чтобы соотвествовали длинне сниппета
    }

    /**
     * получить самый релевантный сниппет
     * @return
     */
    public String getSnippet() {
        StringBuilder builder = new StringBuilder();
        snippetsSet.stream().takeWhile(i -> !lemmas.isEmpty()).filter(i -> i.getLemmaSet().stream()
                .anyMatch(lemmas::contains))
                .map(snippet -> {
                    lemmas.removeAll(snippet.getLemmaSet());
                    String string = snippetToString(snippet);
                    snippet.setSnippet(string);
                    return string;
                }).forEach(builder::append);

        if (!lemmas.isEmpty()) {
            builder.append("<br/>Не найдено: <s>");
            builder.append(String.join("</s>, <s>", lemmas));
            builder.append(".");
        }

        String snippet = builder.toString();
        // заменить раздель тэга
        return snippet.replaceAll("\n", " * ");
    }


    private void addPrefix(Match match, StringBuilder builder) {
        String part = (match.start - prefixSnippetLength > -1 ?
                documentText.substring(match.start - prefixSnippetLength, match.start) :
                documentText.substring(0, match.start)).trim();
        String[] sentenceSplit = part.split("[\n?!.]+");  // "[?!.](\\s+|$)"
        if (sentenceSplit.length != 1) {
            builder.append(sentenceSplit[sentenceSplit.length - 1]);
        } else {
            String[] wordSplit = part.split("\\s+");  // "[?!.](\\s+|$)"
            String prefix = Arrays.stream(wordSplit, 1, wordSplit.length).collect(Collectors.joining(" "));
            builder.append(prefix);
        }
        builder.append(" ");
    }

    private void addPostfix(Match match, StringBuilder builder) {
        String part = (match.end + prefixSnippetLength < documentText.length() - 1 ?
                documentText.substring(match.end, match.end + prefixSnippetLength) :
                documentText.substring(match.end)).trim();
        String[] sentenceSplit = part.split("[\n?!.]+");   // "[?!.](\\s+|$)"

        // TODO: не всегда срабатывает
        builder.append(" ");

        if (sentenceSplit.length != 1) {
            builder.append(sentenceSplit[0]);
        } else {
            String[] wordSplit = part.split("\\s+");  // "[?!.](\\s+|$)"
            String postfix = Arrays.stream(wordSplit, 0, wordSplit.length - 1).collect(Collectors.joining(" "));
            builder.append(postfix);
        }

        // TODO: добавить ...
    }

    private void addBoldWord(Match match, StringBuilder builder) {
        builder.append("<b>");
        builder.append(documentText, match.start, match.end);
        builder.append("</b>");
    }

    private void addBoldWord(String text, Integer start, Integer end, StringBuilder builder) {
        builder.append("<b>");
        builder.append(text, start, end);
        builder.append("</b>");
    }

    /**
     * получить строку из сниппета
     * @param snippet
     * @return
     */
    private String snippetToString(Snippet snippet) {
        TreeSet<Match> matches = snippet.getMatchesSet();
        if (matches.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        Match first = matches.first();
        Match last = matches.last();
        if (first.equals(last)) {
            addPrefix(first, builder);
            addBoldWord(first, builder);
            addPostfix(first, builder);
        } else {
            addPrefix(first, builder);
            String midFix = documentText.substring(first.getStart(), last.getEnd());
            int offset = first.getStart();
            Match[] matchesArray2 = matches.toArray(new Match[0]);
            IntStream.range(0, matchesArray2.length).forEach(i -> {
                Match match = matchesArray2[i];
                if (i > 0) {
                    Match prevMatch = matchesArray2[i - 1];
                    builder.append(midFix, prevMatch.getEnd() - offset, match.getStart() - offset);
                }
                addBoldWord(midFix, match.getStart() - offset, match.getEnd() - offset, builder);
            });
            addPostfix(first, builder);
        }

        // TODO: если предложение в сниппете не закончено и это не законченный тэг добавить..
        //  и если между "\n..text..\n" нет <b></b> убрать это из сниппета т.к. это тэг без совпадения
        //  или пределать findSnippets mathesToSnippets пропускать такие тэги и сразу собирать сниппет в строку там
        builder.substring(builder.length() - 2);
        if (!builder.substring(builder.length() - 2).equals("\n")) {
            builder.append(" ... ");
        }
        return builder.toString();
    }

    /**
     * создать snippet от кждого match
     * snippet включает в себя match которые расположены рядом с первым match в тексте
     * match - это позиция найденного слова в тексте
     * @param matches
     * @return
     */
    private Set<Snippet> mathesToSnippets(Set<Match> matches) {
        Set<Snippet> snippets = new HashSet<>();

        Match[] matchesArray = matches.toArray(new Match[0]);
        IntStream.range(0, matches.size()).forEach(iArray -> {
            Snippet snippet = new Snippet();
            snippetsSet.add(snippet);

            int usedSymbols = 0;
            int offsetMatch = 0;
            do {
                Match match = matchesArray[iArray + offsetMatch];
                snippet.addMatch(match);
                usedSymbols += match.getEnd();
            } while (iArray + ++offsetMatch < matches.size() - 1 && usedSymbols < maxSnippetLength - prefixSnippetLength * 2);

            //snippet.setSnippet(snippetToString(snippet));
        });
        return snippets;
    }

}
