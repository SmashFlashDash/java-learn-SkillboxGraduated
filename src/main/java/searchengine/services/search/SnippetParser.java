package searchengine.services.search;

import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.services.utils.JsoupUtil;
import searchengine.services.utils.LemmaFinder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public class SnippetParser {
    @Setter
    private static int maxSnippetLength;
    private final LemmaFinder lf;
    private final Set<String> lemmas;
    private final Set<Snippet> snippetsSet = new TreeSet<>();
    private final String text;

    public SnippetParser(Document document, LemmaFinder lf, Set<String> lemmas) {
        this.lf = lf;
        this.lemmas = new HashSet<>(lemmas);

        Elements elements = JsoupUtil.documentContnetSelector(document);
        this.text = String.join("\n", elements.eachText());
        findSnippets();
    }

    /**
     * находит match слова которые подходят леммы, затем собирает совпадения в обьекты Snippet, каждый из которых
     * содержит коллекциию совпадений расположенные на заданную длинну сниппета
     */
    private void findSnippets() {
        List<Match> matches = new ArrayList<>();
        Pattern.compile("[А-Яа-я]+").matcher(text).results().forEach(match -> {
            lemmas.stream().filter(l -> lf.isLemmaApplyWord(l, match.group().toLowerCase(Locale.ROOT))).findFirst().ifPresent(lemma -> {
                matches.add(new Match(match.start(), match.end(), lemma, match.group()));
            });
        });

        IntStream.range(0, matches.size()).forEach(iArray -> {
            Snippet snippet = new Snippet();
            snippetsSet.add(snippet);
            int offset = 0;
            boolean isAdded;
            do {
                Match match = matches.get(iArray + offset);
                isAdded = snippet.addMatch(match);
            } while (isAdded && iArray + ++offset < matches.size() - 1);
        });
    }

    public String getSnippet() {
        StringBuilder builder = new StringBuilder();
        snippetsSet.stream().takeWhile(i -> !lemmas.isEmpty() || builder.length() < maxSnippetLength)
                .filter(i -> i.getLemmaSet().stream().anyMatch(lemmas::contains) &&
                        builder.length() + i.getSnippetLength() < maxSnippetLength)
                .map(snippet -> {
                    lemmas.removeAll(snippet.getLemmaSet());
                    String string = new SnippetFormatter(snippet, text).toString();
                    snippet.setText(string);
                    return string;
                }).forEach(builder::append);

        if (!lemmas.isEmpty()) {
            builder.append("<br/>Не найдено: <s>");
            builder.append(String.join("</s>, <s>", lemmas));
            builder.append(".");
        }
        return builder.toString().replace("\n", " * ");
    }
}
