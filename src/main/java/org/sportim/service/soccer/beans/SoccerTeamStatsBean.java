package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.SportType;
import org.sportim.service.beans.stats.TeamStatsBean;

/**
 * Bean for team stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerTeamStatsBean extends TeamStatsBean {
    public int goals = 0;
    public int goalsAgainst = 0;
    public int shots = 0;
    public int shotsOnGoal = 0;
    public int fouls = 0;
    public int yellow = 0;
    public int red = 0;
    public int saves = 0;

    public SoccerTeamStatsBean(int teamID) {
        super(SportType.SOCCER, teamID);
    }
}
