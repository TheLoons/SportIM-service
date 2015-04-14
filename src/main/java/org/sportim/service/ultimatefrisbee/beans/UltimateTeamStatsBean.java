package org.sportim.service.ultimatefrisbee.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.SportType;
import org.sportim.service.beans.stats.TeamStatsBean;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class UltimateTeamStatsBean extends TeamStatsBean {
    public int pointsFor = 0;
    public int pointsAgainst = 0;
    public int fouls = 0;

    public UltimateTeamStatsBean(int teamID) {
        super(SportType.ULTIMATE_FRISBEE, teamID);
    }
}
