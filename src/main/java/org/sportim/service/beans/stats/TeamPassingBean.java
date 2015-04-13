package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean for player passing stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamPassingBean {
    public int teamID;
    public int totalPasses;
    public List<PassBean> passes;

    public TeamPassingBean() {
        passes = new ArrayList<PassBean>();
    }

    public TeamPassingBean(int id) {
        teamID = id;
        passes = new ArrayList<PassBean>();
    }

    public boolean equals(Object o) {
        if (o instanceof TeamPassingBean) {
            TeamPassingBean other = (TeamPassingBean)o;
            return other.teamID == teamID;
        }
        return false;
    }
}
