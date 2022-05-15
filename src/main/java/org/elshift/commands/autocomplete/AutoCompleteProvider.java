package org.elshift.commands.autocomplete;

import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;

/**
 * Provides an interface for fetching autocomplete candidates from a search query.
 */
public interface AutoCompleteProvider {
    /**
     * Retrieve a list of candidates from a search query.
     *
     * @param query The auto complete query
     * @return A list of candidates
     */
    Command.Choice[] getCandidates(AutoCompleteQuery query);
}
