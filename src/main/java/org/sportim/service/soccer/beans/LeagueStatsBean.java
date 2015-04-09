package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LeagueStatsBean {
    public int leagueID = 0;
    public int topScoringTeam = 0;
    public int topTeamScore = -1;
    public int goals = 0;
    public int shots = 0;
    public int shotsOnGoal = 0;
    public int fouls = 0;
    public int yellow = 0;
    public int red = 0;
    public List<TeamStatsBean> teamStats;

    public LeagueStatsBean (int leagueID) {
        this.leagueID = leagueID;
    }
}
