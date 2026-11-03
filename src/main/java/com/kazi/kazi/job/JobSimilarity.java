package com.kazi.kazi.job;

import com.kazi.kazi.ingestion.normalization.NormalizedJob;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

final class JobSimilarity {

    private static final double TITLE_THRESHOLD = 0.9;
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private JobSimilarity() {
    }

    static boolean isDuplicate(NormalizedJob candidate, Job existing, String candidateLocation) {
        return existing.isRemote() == candidate.remote()
            && locationsMatch(existing.getLocation(), candidateLocation)
            && numbersMatch(existing.getTitle(), candidate.title())
            && titleSimilarity(existing.getTitle(), candidate.title()) >= TITLE_THRESHOLD;
    }

    private static boolean numbersMatch(String left, String right) {
        return NUMBER.matcher(left).results().map(MatchResult::group).toList()
            .equals(NUMBER.matcher(right).results().map(MatchResult::group).toList());
    }

    private static boolean locationsMatch(String existing, String candidate) {
        return existing == null || existing.isBlank()
            || candidate == null || candidate.isBlank()
            || existing.equalsIgnoreCase(candidate);
    }

    private static double titleSimilarity(String left, String right) {
        if (left.equals(right)) {
            return 1;
        }
        int maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 1;
        }
        int distance = levenshtein(left, right);
        return 1 - (double) distance / maxLength;
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitution = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                    Math.min(current[column - 1] + 1, previous[column] + 1),
                    previous[column - 1] + substitution
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
