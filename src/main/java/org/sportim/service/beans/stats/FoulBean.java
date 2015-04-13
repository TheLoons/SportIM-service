package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.validation.constraints.NotNull;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class FoulBean {
    public @NotNull
    String player;
    public int teamID;

    public boolean validate() {
        return (player != null);
    }
}
