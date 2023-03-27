package searchengine.services.search;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
class Snippet implements Comparable<Snippet> {
    private final Set<String> lemmaSet = new HashSet<>();
    private final Set<Match> matchesSet = new TreeSet<>();
    private int countUpCaseLetter = 0;
    @Getter(AccessLevel.PUBLIC)
    @Setter
    private String snippet = "";

    //TODO: начианется ли в сниппете с большой буквы или содержаться ли в matchesList большие буквы

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

//    public static String snippetToString(Snippet snippet, String text) {
//        TreeSet<Match> matches = snippet.getMatchesSet();
//        if (matches.isEmpty()) {
//            return "";
//        }
//
//        StringBuilder builder = new StringBuilder();
//        Match first = matches.first();
//        Match last = matches.last();
//        if (first.equals(last)) {
//            addPrefix(text, first, builder);
//            addBoldWord(text, first, builder);
//            addPostfix(text, first, builder);
//        } else {
//            addPrefix(text, first, builder);
//
//            String midFix = text.substring(first.getStart(), last.getEnd());
//            int offset = first.getStart();
//            Match[] matchesArray2 = matches.toArray(new Match[0]);
//            IntStream.range(0, matchesArray2.length).forEach(i -> {
//                Match match = matchesArray2[i];
//                if (i > 0) {
//                    Match prevMatch = matchesArray2[i - 1];
//                    builder.append(midFix, prevMatch.getEnd() - offset, match.getStart() - offset);
//                }
//                addBoldWord(midFix, match.getStart() - offset, match.getEnd() - offset, builder);
//            });
//
//            addPostfix(text, first, builder);
//        }
//
//        // TODO: если предложение в сниппете не закончено и это не законченный тэг добавить..
//        //  и если между "\n..text..\n" нет <b></b> убрать это из сниппета т.к. это тэг без совпадения
//        //  или пределать findSnippets mathesToSnippets пропускать такие тэги и сразу собирать сниппет в строку там
////        builder.substring(builder.length() - 2);
//        if (!builder.substring(builder.length() - 2).equals("\n")) {
//            builder.append(" ... ");
//        }
//        return builder.toString();
//    }
//
//    private static void addPrefix(String text, Match match, StringBuilder builder) {
//        String part = (match.start - prefixSnippetLength > -1 ?
//                text.substring(match.start - prefixSnippetLength, match.start) :
//                text.substring(0, match.start)).trim();
//        String[] sentenceSplit = part.split("[\n?!.]+");  // "[?!.](\\s+|$)"
//        if (sentenceSplit.length != 1) {
//            builder.append(sentenceSplit[sentenceSplit.length - 1]);
//        } else {
//            String[] wordSplit = part.split("\\s+");  // "[?!.](\\s+|$)"
//            String prefix = Arrays.stream(wordSplit, 1, wordSplit.length).collect(Collectors.joining(" "));
//            builder.append(prefix);
//        }
//        builder.append(" ");
//    }
//
//    private static void addPostfix(String text, Match match, StringBuilder builder) {
//        String part = (match.end + prefixSnippetLength < text.length() - 1 ?
//                text.substring(match.end, match.end + prefixSnippetLength) :
//                text.substring(match.end)).trim();
//        String[] sentenceSplit = part.split("[\n?!.]+");   // "[?!.](\\s+|$)"
//
//        // TODO: не всегда срабатывает
//        builder.append(" ");
//
//        if (sentenceSplit.length != 1) {
//            builder.append(sentenceSplit[0]);
//        } else {
//            String[] wordSplit = part.split("\\s+");  // "[?!.](\\s+|$)"
//            String postfix = Arrays.stream(wordSplit, 0, wordSplit.length - 1).collect(Collectors.joining(" "));
//            builder.append(postfix);
//        }
//
//        // TODO: добавить ...
//    }
//
//    private static void addBoldWord(String text, Match match, StringBuilder builder) {
//        builder.append("<b>");
//        builder.append(text, match.start, match.end);
//        builder.append("</b>");
//    }
//
//    private static void addBoldWord(String text, Integer start, Integer end, StringBuilder builder) {
//        builder.append("<b>");
//        builder.append(text, start, end);
//        builder.append("</b>");
//    }
}

