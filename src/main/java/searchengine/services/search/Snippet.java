package searchengine.services.search;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
class Snippet implements Comparable<Snippet> {
    private final Set<String> lemmaSet = new HashSet<>();
    private final Set<Match> matchesSet = new TreeSet<>();
    private int countUpCaseLetter = 0;
    @Getter(AccessLevel.PUBLIC)
    @Setter
    private String snippet = "";

    public void addMatch(Match match) {
        String lemma = match.getLemma();
        lemmaSet.add(lemma);
        matchesSet.add(match);
        if (Character.isUpperCase(match.getWord().charAt(0))) {
            countUpCaseLetter++;
        }
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
                .thenComparing(Snippet::getCountUpCaseLetter)   // начинается ли mathces с большой буквы
                .thenComparing(Snippet::getMatchesSet, Comparator.comparing(Set::size))
                .thenComparing(Snippet::getSnippet, Comparator.comparingInt(String::length))
                .compare(o, this);
    }
}

