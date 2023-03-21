package searchengine.services.searchClasses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import searchengine.services.searchClasses.SnippetMatch;

import java.util.TreeSet;

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

// с простым вариантом у нас start единый массив, и там можно считать по index
// только чтобы разделить по предложениям между тегами надо брать wholeText чтобы был \n
@NoArgsConstructor
class SnippetDtoSimple {
    @Getter
    private final TreeSet<SnippetMatchSimple> matchers = new TreeSet<>();
    @Getter
    @Setter
    private Float maxTagRelevance = null;

    public void addMatch(SnippetMatchSimple match) {
        matchers.add(match);
    }

    public boolean isMatchesEmpty() {
        return matchers.isEmpty();
    }
}
