package org.sportim.service.api;

import org.sportim.service.beans.stats.AbstractTeamResultsBean;

import java.util.List;
import java.util.SortedSet;

/**
 * Interface that should be extended by sports-specific table APIs
 */
public interface TableAPI {

    /**
     * Get the list of teams for a set of events, sorted by rank
     * @param events the list of event IDs
     * @return the sorted set of teams, or null if an error occurred
     */
    public SortedSet<AbstractTeamResultsBean> getTableForEvents(List<Integer> events);
}
