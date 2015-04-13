package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AggregateEventBean {
    public AggregateEventBean(int eventID) {
        this.eventID = eventID;
    }

    public StatsType type = StatsType.UNKNOWN;
    public int eventID = -1;
}
