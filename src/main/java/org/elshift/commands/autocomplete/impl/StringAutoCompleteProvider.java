package org.elshift.commands.autocomplete.impl;

import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.elshift.commands.autocomplete.AutoCompleteProvider;

import java.util.List;
import java.util.Map;

/**
 * A generic auto-complete provider for string options
 */
public abstract class StringAutoCompleteProvider implements AutoCompleteProvider {
    /**
     * @return A list of strings to search through. This value should be cached for large arrays
     */
    protected abstract List<String> getPossibleValues();

    /**
     * @return The maximum amount of search candidates
     */
    protected abstract int getMaxCandidates();

    @Override
    public Command.Choice[] getCandidates(AutoCompleteQuery query) {
        if (query.getType() != OptionType.STRING)
            return null;

        List<String> possibleValues = getPossibleValues();

        if (possibleValues.isEmpty())
            return null;

        String search = query.getValue();

        // Keep track of the best six matches
        Map<Integer, String> bestMatches = new java.util.TreeMap<>();

        final int maxCandidates = getMaxCandidates();

        // Find the best matches
        for (String possibleValue : possibleValues) {
            int similarity = similarity(possibleValue, search);
            if (similarity > 0) {
                bestMatches.put(similarity, possibleValue);
            }

            if (bestMatches.size() > maxCandidates) {
                bestMatches.remove(bestMatches.keySet().iterator().next());
            }
        }

        // Convert the best matches to choices (reverse order)
        Command.Choice[] choices = new Command.Choice[bestMatches.size()];
        int i = 0;
        for (String possibleValue : bestMatches.values()) {
            choices[choices.length - i - 1] = new Command.Choice(possibleValue, possibleValue);
            i++;
        }

        return choices;
    }

    private static int similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1; /* both strings are zero length */
        }
        return (int) (((longerLength - editDistance(longer, shorter)) / (double) longerLength) * 100);
    }

    private static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}
