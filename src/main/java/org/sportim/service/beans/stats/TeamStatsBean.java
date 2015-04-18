package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.SportType;

import java.util.List;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamStatsBean extends StatsBean {
    public int teamID = 0;
    public List<PlayerStatsBean> playerStats = null;

    public TeamStatsBean(SportType sport, int teamID) {
        this.teamID = teamID;
        this.type = sport;
    }
}
