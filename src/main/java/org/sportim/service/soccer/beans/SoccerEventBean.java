package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.SportType;

import java.util.List;

/**
 * Bean for returning stats for an event
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerEventBean extends AggregateEventBean {
    public int totalGoals = 0;
    public int totalShots = 0;
    public int totalShotsOnGoal = 0;
    public List<SoccerTeamStatsBean> teamStats = null;

    public SoccerEventBean(int eventID) {
        super(eventID);
        this.type = SportType.SOCCER;
    }
}
