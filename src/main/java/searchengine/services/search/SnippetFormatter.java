package searchengine.services.search;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnippetFormatter {
    private final static Pattern patternSentence = Pattern.compile("[\n?!.]+");
    private final static Pattern patternUpWord = Pattern.compile("(\\b[А-Я])");
    private final Snippet snippet;
    private final int suffixLength = 100;
    private final String text;

    public SnippetFormatter(Snippet snippet, String text) {
        this.snippet = snippet;
        this.text = text;
    }

    @Override
    public String toString() {
        TreeSet<Match> matches = snippet.getMatchesSet();

        StringBuilder builder = new StringBuilder();
        Match first = matches.first();
        Match last = matches.last();
        int start = Math.max(first.getStart() - suffixLength, 0);
        int end = Math.min(last.getEnd() + suffixLength, text.length());
        String newPrefix = trimPrefix(text.substring(start, first.getStart()));
        String newPostfix = trimPostfix(text.substring(last.getEnd(), end));

        builder.append(newPrefix);
        if (first.equals(last)) {
            addBoldWord(text, first, builder);
        } else {
            addMatchesToSuffix(first, last, matches, builder);
        }
        builder.append(newPostfix);

        if (!builder.substring(builder.length() - 2).equals("\n")) {
            builder.append(" ... ");
        }
        return builder.toString();
    }

    private String trimPrefix(String part) {
        String[] sentenceSplit = part.split(patternSentence.pattern());
        if (sentenceSplit.length > 1) {
            return sentenceSplit[sentenceSplit.length - 1];
        }
        List<MatchResult> mathces = patternUpWord.matcher(part).results().collect(Collectors.toList());
        if (!mathces.isEmpty()) {
            MatchResult matchResult = mathces.get(mathces.size() - 1);
            return part.substring(matchResult.start());
        }
        String[] spaceSplit = part.split("\\s+");
        if (spaceSplit.length > 1) {
            return Arrays.stream(spaceSplit, 1, spaceSplit.length).collect(Collectors.joining(" ")).concat(" ");
        }
        return part;
    }

    private String trimPostfix(String part) {
        String[] sentenceSplit = part.split(patternSentence.pattern());
        if (sentenceSplit.length > 1) {
            return sentenceSplit[0];
        }
        List<MatchResult> mathces = patternUpWord.matcher(part).results().collect(Collectors.toList());
        if (!mathces.isEmpty()) {
            MatchResult matchResult = mathces.get(0);
            return part.substring(0, matchResult.start());
        }
        String[] spaceSplit = part.split("\\s+");
        if (spaceSplit.length > 1) {
            return " ".concat(Arrays.stream(spaceSplit, 0, spaceSplit.length - 1).collect(Collectors.joining(" ")));
        }
        return part;
    }


    // TODO: при начале парсинга перерасчитать длинну suffixLength, на кол-во совпадений
    //  должен быть общий длинна суффикс ленгс на сниипеет
    // тоесть если сниппет имеет один matcher его длинна должны пересчитывать а если несколько матчеров
    // тоже пересчитывается
    private void addMatchesToSuffix(Match first, Match last, Set<Match> matches, StringBuilder builder) {
        String midFix = text.substring(first.getStart(), last.getEnd());
        Match[] matchesArray = matches.toArray(new Match[0]);
        Match prevMatch = null;
        int offset = first.getStart();
        for (Match match : matchesArray) {
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

}
