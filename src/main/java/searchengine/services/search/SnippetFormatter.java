package searchengine.services.search;

import java.util.Arrays;
import java.util.List;
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
        List<Match> matches = snippet.getMatches();

        StringBuilder builder = new StringBuilder();
        Match first = matches.get(0);
        Match last = matches.get(matches.size() - 1);
        int start = Math.max(first.getStart() - suffixLength, 0);
        int end = Math.min(last.getEnd() + suffixLength, text.length());
        String newPrefix = trimPrefix(text.substring(start, first.getStart()));
        String newPostfix = trimPostfix(text.substring(last.getEnd(), end));

        builder.append(newPrefix);
        if (first.equals(last)) {
            addBoldWord(text, first, builder);
        } else {
            addMatchesToSuffix(matches, builder);
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

    private void addMatchesToSuffix(Iterable<Match> matches, StringBuilder builder) {
        Match prevMatch = null;
        for (Match match : matches) {
            if (prevMatch != null) {
                builder.append(text, prevMatch.getEnd(), match.getStart());
            }
            addBoldWord(text, match, builder);
            prevMatch = match;
        }
    }

    private void addBoldWord(String text, Match match, StringBuilder builder) {
        builder.append("<b>");
        builder.append(text, match.getStart(), match.getEnd());
        builder.append("</b>");
    }
}
