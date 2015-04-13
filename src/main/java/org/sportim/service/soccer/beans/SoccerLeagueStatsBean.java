package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.StatsType;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerLeagueStatsBean extends LeagueStatsBean {
    public int goals = 0;
    public int shots = 0;
    public int shotsOnGoal = 0;
    public int fouls = 0;
    public int yellow = 0;
    public int red = 0;

    public SoccerLeagueStatsBean(int leagueID) {
        super(leagueID);
        this.type = StatsType.SOCCER;
    }
}
