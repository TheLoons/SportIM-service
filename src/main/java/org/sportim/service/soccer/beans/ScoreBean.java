package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

/**
 * Bean used for passing score information
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ScoreBean {
    public int teamID;
    public int goalieTeamID;
    public @NotNull String player;
    public @NotNull String assist;
    public @NotNull String goalkeeper;

    public boolean validate() {
        return (player != null) && (assist != null) && (goalkeeper != null);
    }
}
