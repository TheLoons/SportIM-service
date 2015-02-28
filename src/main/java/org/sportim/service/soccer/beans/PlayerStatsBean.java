package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Bean for player stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class PlayerStatsBean {
    public String login;
    public int goals = 0;
    public int assists = 0;
    public int goalsAgainst = 0;
    public int shots = 0;
    public int shotsOnGoal = 0;
    public int fouls = 0;
    public int yellow = 0;
    public int red = 0;
    public int saves = 0;
    public int minutes = 0;
}
