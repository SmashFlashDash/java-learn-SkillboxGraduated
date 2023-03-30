package searchengine.services.search;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.LemmaFinder;
import searchengine.config.SearchConfig;
import searchengine.services.utils.JsoupUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SnippetParser {
    private final Document document;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Set<Snippet> snippetsSet = new TreeSet<>();
    private final String text;
    @Setter
    private static int maxSnippetLength;
    private static final int suffixLength = 50;

    // TODO: надо тогда леммы в бд записывать тоже только из content
    public SnippetParser(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = new HashSet<>(lemmas);

        // фильтрация элементов сайта, текст может быть в div и содержать ссылки
        Elements elements = JsoupUtil.documentContnetSelector(document);
        this.text = String.join("\n", elements.eachText());
//        this.text = elements.text();
        findSnippets();
    }

    /**
     * поиск сниппета по всему тексту документа
     */
    private void findSnippets() {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String[] lowWords = lowerText.replaceAll("([^а-я\\s])", " ").trim().split("\\s+");

        // находим все леммы на странице записываем их в mathces
        Set<Match> matches = new TreeSet<>();
        Arrays.stream(lowWords).forEach(word -> {
            lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, word)).findFirst().ifPresent(lemma -> {
                // TODO: здесб не надо делать find по всем словам, нужно получить именно то слово по которому идем
                //  по сути мы сплитанули слова по пробелам можно идти стримо по mathces \\s+
                //  и кидать их в mathes
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                matches.addAll(m.results()
                        .map(i -> new Match(i.start(), i.end(), lemma, text.substring(i.start(), i.end())))
                        .collect(Collectors.toSet()));
            });
        });

        // собираем из matches snippet
        Match[] matchesArray = matches.toArray(new Match[0]);
        int snippetLength = suffixLength + maxSnippetLength;
        IntStream.range(0, matches.size()).forEach(iArray -> {
            Snippet snippet = new Snippet();
            snippetsSet.add(snippet);
            int usedSymbols;
            int offsetMatch = 0;
            do {
                Match match = matchesArray[iArray + offsetMatch];
                snippet.addMatch(match);
                usedSymbols = match.getEnd();
                snippet.getLemmaSet().size();
            } while (iArray + ++offsetMatch < matches.size() - 1 && usedSymbols < snippetLength);
            //snippet.setSnippet(snippetToString(snippet));
        });
    }

    public String getSnippet() {
        // TODO: как определить длинну сниппета, для одних и нескольких лемм
        // TODO: правилен ли порядок сниппетов в snippetsSet
        StringBuilder builder = new StringBuilder();
        snippetsSet.stream().takeWhile(i -> !lemmas.isEmpty()).filter(i -> i.getLemmaSet().stream().anyMatch(lemmas::contains))
                .map(snippet -> {
                    lemmas.removeAll(snippet.getLemmaSet());
                    String string = new SnippetFormatter(snippet, text).toString();
                    snippet.setSnippet(string);
                    return string;
                }).forEach(builder::append);

        if (!lemmas.isEmpty()) {
            builder.append("<br/>Не найдено: <s>");
            builder.append(String.join("</s>, <s>", lemmas));
            builder.append(".");
        }
        return builder.toString().replaceAll("\n", " * ");
    }
}
