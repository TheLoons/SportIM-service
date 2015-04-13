package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class PlayerStatsBean extends StatsBean {
    public String login;

    public PlayerStatsBean(String login) {
        this.login = login;
    }
}
