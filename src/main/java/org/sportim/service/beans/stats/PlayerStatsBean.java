package org.sportim.service.beans.stats;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.SportType;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class PlayerStatsBean extends StatsBean {
    public String login;

    public PlayerStatsBean(SportType sport, String login) {
        this.type = sport;
        this.login = login;
    }
}
