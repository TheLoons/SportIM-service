package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Bean for returning goals for an event
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AggregateEventBean {
    public int totalGoals = 0;
    public int totalShots = 0;
    public int totalShotsOnGoal = 0;
    public List<TeamStatsBean> teamStats = null;
}
