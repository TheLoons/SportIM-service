package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.FoulBean;

import javax.validation.constraints.NotNull;

/**
 * Bean for fouls/cards
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SoccerFoulBean extends FoulBean {
    public boolean yellow;
    public boolean red;
}
