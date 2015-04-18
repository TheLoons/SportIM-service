package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.AbstractTeamResultsBean;
import org.sportim.service.util.SportType;

/**
 * Bean for table results
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerTeamResultsBean extends AbstractTeamResultsBean {
    public int goalsFor = 0;
    public int goalsAgainst = 0;
    public int points = 0;

    public SoccerTeamResultsBean(int teamID) {
        super(SportType.SOCCER, teamID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SoccerTeamResultsBean) {
            SoccerTeamResultsBean other = (SoccerTeamResultsBean)o;
            return teamID == other.teamID;
        }
        return false;
    }

    @Override
    public int compareTo(AbstractTeamResultsBean abstractTeamResultsBean) {
        if (!(abstractTeamResultsBean instanceof SoccerTeamResultsBean)) {
            return 0;
        }
        SoccerTeamResultsBean teamResultsBean = (SoccerTeamResultsBean)abstractTeamResultsBean;

        if (teamID == teamResultsBean.teamID) {
            return 0;
        }

        if (points > teamResultsBean.points) {
            return -1;
        }
        if (points < teamResultsBean.points) {
            return 1;
        }
        // Tie breakers
        if (wins > teamResultsBean.wins) {
            return -1;
        } else if (wins < teamResultsBean.wins) {
            return 1;
        }

        if (losses < teamResultsBean.losses) {
            return -1;
        } else if (losses > teamResultsBean.losses) {
            return 1;
        }

        if (goalsFor > teamResultsBean.goalsFor) {
            return -1;
        } else if (goalsFor < teamResultsBean.goalsFor) {
            return 1;
        }

        if (goalsAgainst < teamResultsBean.goalsAgainst) {
            return -1;
        } else if (goalsAgainst > teamResultsBean.goalsAgainst) {
            return 1;
        }

        return (teamID < teamResultsBean.teamID) ? -1 : 1;
    }
}