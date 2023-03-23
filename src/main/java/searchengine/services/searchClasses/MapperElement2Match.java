package searchengine.services.searchClasses;

import lombok.*;
import org.jsoup.nodes.Element;

import java.util.*;

@Getter(AccessLevel.PRIVATE)
class Snippet implements Comparable<Snippet> {
    int countStartUpCse = 0;
    private final Set<String> lemmaSet = new HashSet<>();      // какие леммы в элемента, дублирует matchesMap.keys()
    private final Set<Match> matchesSet = new TreeSet<>();    // упорядоченая коллекция слов в элемента
    // вариант мэп, минус что все совпадения не совпадают
    //private final Map<String, List<Match>> matchesMap = new HashMap<>(); // держит match по каждой лемме но неупорядолчено по тексту элемента
    @Getter(AccessLevel.PUBLIC)
    @Setter
    private String snippet = "";

    //TODO: начианется ли в сниппете с большой буквы или содержаться ли в matchesList большие буквы

    public void addMatch(Match match) {
        String lemma = match.getLemma();
        lemmaSet.add(lemma);
        matchesSet.add(match);
        if (match.isStartUpLetter) {
            countStartUpCse++;
        }
//        if (matchesMap.containsKey(lemma)) {
//            matchesMap.get(lemma).add(match);
//        } else {
//            matchesMap.put(lemma, new ArrayList<>(Collections.singletonList(match)));
//        }
    }

    public TreeSet<Match> getMatchesSet() {
        return new TreeSet<>(matchesSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Snippet)) return false;
        Snippet snippet = (Snippet) o;
        return lemmaSet.equals(snippet.lemmaSet) &&
                matchesSet.equals(snippet.matchesSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchesSet);
    }

    @Override
    public int compareTo(Snippet o) {
        return Comparator.comparing(Snippet::getLemmaSet, Comparator.comparingInt(Set::size))
                .thenComparing(Snippet::getCountStartUpCse)   // начинается ли сниппет с большой буквы
                .thenComparing(Snippet::getMatchesSet, Comparator.comparing(Set::size))
                .thenComparing(Snippet::getSnippet, Comparator.comparingInt(String::length))
                .compare(o, this);
    }
}

@Getter
@AllArgsConstructor
class MapperElement2Match implements Comparable<MapperElement2Match> {
    private final int tagId;
    private final Float tagRelevance;
    private final Element element;
    // вариант со словами по порядку
    private final Set<String> lemmaSet = new HashSet<>();      // какие леммы в элемента, дублирует matchesMap.keys()
    private final Set<Match> matchesList = new TreeSet<>();    // упорядоченая коллекция слов в элемента
    // вариант мэп, минус что все совпадения не совпадают
    private final Map<String, List<Match>> matchesMap = new HashMap<>(); // держит match по каждой лемме но неупорядолчено по тексту элемента

    public void addMatch(Match match) {
        String lemma = match.getLemma();
        lemmaSet.add(lemma);
        matchesList.add(match);
        if (matchesMap.containsKey(lemma)) {
            matchesMap.get(lemma).add(match);
        } else {
            matchesMap.put(lemma, new ArrayList<>(Collections.singletonList(match)));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapperElement2Match that = (MapperElement2Match) o;
        return element == that.element &&
                matchesList.equals(that.getMatchesList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    @Override
    public int compareTo(MapperElement2Match o) {
        return Comparator.comparing(MapperElement2Match::getLemmaSet, Comparator.comparingInt(Set::size))
                .thenComparing(MapperElement2Match::getTagRelevance)
                .thenComparing(MapperElement2Match::getMatchesList, Comparator.comparing(Set::size))
                .thenComparing(MapperElement2Match::getTagId)
                .compare(o, this);
    }
}

@Setter
@Getter
class Match implements Comparable<Match> {
    int start;
    int end;
    String lemma;
    String word;
    boolean isStartUpLetter;

    public Match(int start, int end, String lemma) {
        this.start = start;
        this.end = end;
        this.lemma = lemma;
    }

    public Match(int start, int end, String lemma, String word) {
        this.start = start;
        this.end = end;
        this.lemma = lemma;
        this.word = word;
        this.isStartUpLetter = Character.isUpperCase(word.charAt(0));
    }

    @Override
    public int compareTo(Match o) {
        return Comparator.comparing(Match::getStart).compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return start == match.start &&
                end == match.end &&
                lemma.equals(match.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start);
    }
}
