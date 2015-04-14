package org.sportim.service.api;

import org.sportim.service.beans.stats.AbstractTeamResultsBean;

import java.util.List;
import java.util.SortedSet;

public interface TableAPI {
    public SortedSet<AbstractTeamResultsBean> getTableForEvents(List<Integer> events);
}
