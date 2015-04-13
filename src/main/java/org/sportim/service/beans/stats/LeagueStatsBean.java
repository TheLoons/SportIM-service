package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LeagueStatsBean extends StatsBean {
    public int leagueID = 0;
    public int topScoringTeam = 0;
    public int topTeamScore = -1;
    public List<TeamStatsBean> teamStats;

    public LeagueStatsBean (int leagueID) {
        this.leagueID = leagueID;
    }
}
