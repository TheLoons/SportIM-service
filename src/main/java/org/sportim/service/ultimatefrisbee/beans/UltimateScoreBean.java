package org.sportim.service.ultimatefrisbee.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class UltimateScoreBean {
    public int teamID;
    public int opposingTeamID;
    public @NotNull String thrower;
    public @NotNull String receiver;

    public boolean validate() {
        return (thrower != null) && (receiver != null) && (teamID > 0) && (opposingTeamID > 0);
    }
}
