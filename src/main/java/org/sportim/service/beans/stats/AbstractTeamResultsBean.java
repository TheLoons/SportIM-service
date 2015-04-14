package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class AbstractTeamResultsBean implements Comparable<AbstractTeamResultsBean> {
    public SportType type = SportType.UNKNOWN;
    public int teamID = 0;
    public int rank = 0;
    public int wins = 0;
    public int losses = 0;
    public int ties = 0;

    public AbstractTeamResultsBean(SportType sport, int teamID) {
        this.type = sport;
        this.teamID = teamID;
    }
}
