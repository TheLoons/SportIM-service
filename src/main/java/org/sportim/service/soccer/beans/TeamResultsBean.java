package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Bean for table results
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamResultsBean implements Comparable<TeamResultsBean> {
    int teamID = 0;
    int rank = 0;
    int wins = 0;
    int losses = 0;
    int ties = 0;
    int goalsFor = 0;
    int goalsAgainst = 0;
    int points = 0;

    @Override
    public boolean equals(Object o) {
        if (o instanceof  TeamResultsBean) {
            TeamResultsBean other = (TeamResultsBean)o;
            return teamID == other.teamID;
        }
        return false;
    }

    @Override
    public int compareTo(TeamResultsBean teamResultsBean) {
        if (points > teamResultsBean.points) {
            return -1;
        }
        if (points < teamResultsBean.ties) {
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
        } else if (losses < teamResultsBean.losses) {
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

        return 0;
    }
}