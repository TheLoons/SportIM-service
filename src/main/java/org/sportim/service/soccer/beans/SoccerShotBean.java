package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

/**
 * Bean used for passing score information
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerShotBean {
    public int teamID;
    public int goalieTeamID;
    public @NotNull String player;
    public String goalkeeper;
    public boolean onGoal;

    public boolean validate() {
        return (player != null) && (!onGoal || goalkeeper != null);
    }
}
