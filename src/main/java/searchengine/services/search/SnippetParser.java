package searchengine.services.search;

import lombok.Getter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.LemmaFinder;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SnippetParser {
    private final static Pattern patternSentence = Pattern.compile("[\n?!;]+");
    private final Document document;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Set<Snippet> snippetsSet = new TreeSet<>();
    private final String text;
    private final int maxSnippetLength = 200;
    private final int suffixLength = 50;

    // TODO: надо тогда леммы в бд записывать тоже только из content
    public SnippetParser(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.document = document;
        this.lf = lf;
        this.lemmas = new HashSet<>(lemmas);

        // фильтрация элементов сайта, текст может быть в div и содержать ссылки
        Elements elements = document.select("div[class*=content]")
                .not("nav, aside, header, footer, [class*=menu]");
//                .select("p, ul, ol");
//                .select("h1 ~ *, h2 ~ *, h3 ~ *, h4 ~ *");

//        this.text = String.join("\n", elements.stream().map(Element::wholeText)
//                .collect(Collectors.toCollection(LinkedHashSet::new)))
//                .replaceAll("\\n+\\s+", "\n");
//        Elements elements = document.getAllElements();
        this.text = String.join("\n", elements.eachText());
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
                Matcher m = Pattern.compile(String.format("\\b%s\\b", word), Pattern.CASE_INSENSITIVE).matcher(lowerText);
                matches.addAll(m.results()
                        .map(i -> new Match(i.start(), i.end(), lemma, text.substring(i.start(), i.end())))
                        .collect(Collectors.toSet()));
            });
        });

        // собираем из matches snippet
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
            } while (iArray + ++offsetMatch < matches.size() - 1 && usedSymbols < maxSnippetLength - suffixLength * 2);
            //snippet.setSnippet(snippetToString(snippet));
        });
    }

    // TODO: тут мб ошибка
    /**
     * получить самый релевантный сниппет
     * @return
     */
    public String getSnippet() {
        // TODO: как определить длинну сниппета, для одних и нескольких лемм
        // TODO: правилен ли порядок сниппетов в snippetsSet
        // findFirst или FindAny
        StringBuilder builder = new StringBuilder();
        snippetsSet.stream().takeWhile(i -> !lemmas.isEmpty()).filter(i -> i.getLemmaSet().stream().anyMatch(lemmas::contains))
                .map(snippet -> {
                    lemmas.removeAll(snippet.getLemmaSet());
                    String string = snippetToString(snippet);
                    snippet.setSnippet(string);
                    return string;
                }).forEach(i -> {builder.append("\n").append(i);});

        if (!lemmas.isEmpty()) {
            builder.append("<br/>Не найдено: <s>");
            builder.append(String.join("</s>, <s>", lemmas));
            builder.append(".");
        }
        String snippet = builder.substring(1);
        return snippet.replaceAll("\n", " * ");
    }

    private void addPrefix(Match match, StringBuilder builder) {
        String part = (match.getStart() - suffixLength > -1 ?
                text.substring(match.getStart() - suffixLength, match.getStart()) :
                text.substring(0, match.getStart())).trim();
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
        builder.append(" ");
        String part = (match.getEnd() + suffixLength < text.length() - 1 ?
                text.substring(match.getEnd(), match.getEnd() + suffixLength) :
                text.substring(match.getEnd())).trim();
        MatchResult preFixEnd = patternSentence.matcher(part).results()
                .takeWhile(i -> i.end() < suffixLength).reduce((o1, o2) -> o2).orElse(null);
        if (preFixEnd != null) {
            builder.append(part, 0, preFixEnd.end());
        } else {
            String[] wordSplit = part.split("\\s+");  // "[?!.](\\s+|$)"
            String postfix = Arrays.stream(wordSplit, 0, wordSplit.length - 1).collect(Collectors.joining(" "));
            builder.append(postfix);
            builder.append("...");
        }
    }

    private void addMatchesToSuffix(Match first, Match last, Set<Match> matches, StringBuilder builder) {
        String midFix = text.substring(first.getStart(), last.getEnd());
        Match[] matchesArray = matches.toArray(new Match[0]);
        Match prevMatch = null;
        int offset = first.getStart();
        for(Match match : matchesArray) {
            if (prevMatch != null) {
                builder.append(midFix, prevMatch.getEnd() - offset, match.getStart() - offset);
            }
            addBoldWord(midFix, match, offset, builder);
            prevMatch = match;
        }
    }

    private void addBoldWord(String text, Match match, StringBuilder builder) {
        builder.append("<b>");
        builder.append(text, match.getStart(), match.getEnd());
        builder.append("</b>");
    }

    private void addBoldWord(String text, Match match, int offset, StringBuilder builder) {
        builder.append("<b>");
        builder.append(text, match.getStart() - offset, match.getEnd() - offset);
        builder.append("</b>");
    }

    /**
     * получить строку из сниппета
     * @param snippet
     * @return
     */
    // TODO: вынести в класс Snippet
    private String snippetToString(Snippet snippet) {
        TreeSet<Match> matches = snippet.getMatchesSet();

        StringBuilder builder = new StringBuilder();
        Match first = matches.first();
        Match last = matches.last();

        if (first.equals(last)) {
            addPrefix(first, builder);
            addBoldWord(text, first, builder);
            addPostfix(first, builder);
        } else {
            addPrefix(first, builder);
            addMatchesToSuffix(first, last, matches, builder);
            addPostfix(first, builder);
        }

        // TODO: если предложение в сниппете не закончено и это не законченный тэг добавить..
        //  и если между "\n..text..\n" нет <b></b> убрать это из сниппета т.к. это тэг без совпадения
        //  или пределать findSnippets mathesToSnippets пропускать такие тэги и сразу собирать сниппет в строку там
        // builder.substring(builder.length() - 2);
        // if (!builder.substring(builder.length() - 2).equals("\n")) {
        //     builder.append(" ... ");
        // }
        return builder.toString();
    }

}
