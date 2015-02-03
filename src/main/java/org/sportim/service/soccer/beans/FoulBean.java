package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

/**
 * Bean for fouls/cards
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class FoulBean {
    public @NotNull String player;
    public int teamID;
    public boolean yellow;
    public boolean red;

    public boolean validate() {
        return (player != null);
    }
}
