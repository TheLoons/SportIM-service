package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.SportType;

/**
 * Bean for player stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerPlayerStatsBean extends PlayerStatsBean {
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

    public SoccerPlayerStatsBean(String login) {
        super(SportType.SOCCER, login);
    }
}
