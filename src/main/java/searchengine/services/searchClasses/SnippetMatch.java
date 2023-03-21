package searchengine.services.searchClasses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.nodes.Element;

import java.util.Comparator;
import java.util.Objects;

@Getter
@AllArgsConstructor
class SnippetMatch implements Comparable<SnippetMatch> {
    private final int tagCount;
    private final int start;
    private final int end;
    private final Float tagRelevance;
    private final Element element;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnippetMatch that = (SnippetMatch) o;
        return tagCount == that.tagCount && start == that.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagCount, start);
    }

    @Override
    public int compareTo(SnippetMatch o) {
        return Comparator.comparing(SnippetMatch::getTagCount)
                .thenComparing(SnippetMatch::getStart)
                .compare(this, o);
    }
}

@Getter
@AllArgsConstructor
class SnippetMatchSimple implements Comparable<SnippetMatchSimple> {
    private final int start;
    private final int end;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnippetMatchSimple that = (SnippetMatchSimple) o;
        return start == that.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start);
    }

    @Override
    public int compareTo(SnippetMatchSimple o) {
        return Comparator.comparing(SnippetMatchSimple::getStart)
                .compare(this, o);
    }
}
