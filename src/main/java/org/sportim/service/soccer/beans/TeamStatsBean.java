package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Bean for team stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamStatsBean {
    public int teamID = 0;
    public int goals = 0;
    public int goalsAgainst = 0;
    public int shots = 0;
    public int shotsOnGoal = 0;
    public int fouls = 0;
    public int yellow = 0;
    public int red = 0;
    public int saves = 0;
    public List<PlayerStatsBean> playerStats = null;
}
