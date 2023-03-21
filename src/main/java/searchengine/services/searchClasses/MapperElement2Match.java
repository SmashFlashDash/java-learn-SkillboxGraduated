package searchengine.services.searchClasses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Element;

import java.util.*;

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

    public void addMatch(Match match){
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
@AllArgsConstructor
class Match implements Comparable<Match>{
    int start;
    int end;
    String lemma;
    String word;

    public Match(int start, int end, String lemma) {
        this.start = start;
        this.end = end;
        this.lemma = lemma;
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
                Objects.equals(lemma, match.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start);
    }
}
