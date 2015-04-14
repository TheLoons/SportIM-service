package org.sportim.service.ultimatefrisbee.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.SportType;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class UltimatePlayerStatsBean extends PlayerStatsBean {
    public int pointsThrown = 0;
    public int pointsReceived = 0;
    public int fouls = 0;

    public UltimatePlayerStatsBean(String login) {
        super(SportType.ULTIMATE_FRISBEE, login);
    }
}
