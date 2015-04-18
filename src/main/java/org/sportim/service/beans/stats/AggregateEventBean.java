package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.SportType;

import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AggregateEventBean {
    public AggregateEventBean(SportType sport, int eventID) {
        this.eventID = eventID;
        this.type = sport;
    }

    public SportType type = SportType.UNKNOWN;
    public List<TeamStatsBean> teamStats;
    public int eventID = -1;
}
