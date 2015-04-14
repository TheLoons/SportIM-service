package org.sportim.service.ultimatefrisbee.beans;

import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.SportType;

/**
 * Created by hannah on 4/13/15.
 */
public class UltimateLeagueStatsBean extends LeagueStatsBean {
    public int points = 0;
    public int fouls = 0;

    public UltimateLeagueStatsBean(int leagueID) {
        super(SportType.ULTIMATE_FRISBEE, leagueID);
    }
}
