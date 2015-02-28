package org.sportim.service.soccer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

/**
 * Bean for player passing stats
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class PlayerPassingBean {
    public String player;
    public int totalPassesMade;
    public int totalPassesReceived;
    public Map<String, Integer> passesMade;
    public Map<String, Integer> passesReceived;
}
