package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Bean for table results
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerTeamResultsBean implements Comparable<SoccerTeamResultsBean> {
    public int teamID = 0;
    public int rank = 0;
    public int wins = 0;
    public int losses = 0;
    public int ties = 0;
    public int goalsFor = 0;
    public int goalsAgainst = 0;
    public int points = 0;

    public SoccerTeamResultsBean(int teamID) {
        this.teamID = teamID;
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
    public int compareTo(SoccerTeamResultsBean teamResultsBean) {
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