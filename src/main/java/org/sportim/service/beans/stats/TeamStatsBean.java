package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.soccer.beans.SoccerPlayerStatsBean;

import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamStatsBean extends StatsBean {
    public int teamID = 0;
    public List<SoccerPlayerStatsBean> playerStats = null;

    public TeamStatsBean(int teamID) {
        this.teamID = teamID;
    }
}
