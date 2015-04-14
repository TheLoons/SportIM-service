package org.sportim.service.ultimatefrisbee.beans;

import org.sportim.service.beans.stats.AbstractTeamResultsBean;
import org.sportim.service.beans.stats.SportType;

public class UltimateTeamResultsBean extends AbstractTeamResultsBean {
    public int pointsFor = 0;
    public int pointsAgainst = 0;

    public UltimateTeamResultsBean(int teamID) {
        super(SportType.ULTIMATE_FRISBEE, teamID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof UltimateTeamResultsBean) {
            UltimateTeamResultsBean other = (UltimateTeamResultsBean)o;
            return teamID == other.teamID;
        }
        return false;
    }

    @Override
    public int compareTo(AbstractTeamResultsBean abstractTeamResultsBean) {
        if (!(abstractTeamResultsBean instanceof UltimateTeamResultsBean)) {
            return 0;
        }
        UltimateTeamResultsBean teamResultsBean = (UltimateTeamResultsBean)abstractTeamResultsBean;

        if (teamID == teamResultsBean.teamID) {
            return 0;
        }

        // Difference between win/loss is first
        int winDiff = wins - losses;
        int otherWinDiff = teamResultsBean.wins - teamResultsBean.losses;
        if (winDiff > otherWinDiff) {
            return -1;
        }

        // Tie-breakers
        if (wins > teamResultsBean.wins) {
            return -1;
        }
        if (wins < teamResultsBean.wins) {
            return 1;
        }

        if (losses < teamResultsBean.losses) {
            return -1;
        } else if (losses > teamResultsBean.losses) {
            return 1;
        }

        if (pointsFor > teamResultsBean.pointsFor) {
            return -1;
        } else if (pointsFor < teamResultsBean.pointsFor) {
            return 1;
        }

        if (pointsAgainst < teamResultsBean.pointsAgainst) {
            return -1;
        } else if (pointsAgainst > teamResultsBean.pointsAgainst) {
            return 1;
        }

        return (teamID < teamResultsBean.teamID) ? -1 : 1;
    }
}
