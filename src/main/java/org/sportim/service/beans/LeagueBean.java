package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Doug on 1/19/15.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LeagueBean {

    private int id = 0;
    private String name;
    private String owner;
    private List<TeamBean> teams;
    private String sport;

    public LeagueBean(){
    }

    public LeagueBean(ResultSet rs) throws SQLException {
        id = rs.getInt(1);
        name = rs.getString(2);
        owner = rs.getString(3);
        sport = rs.getString(4);
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getLeagueId() {
        return id;
    }

    public void setLeagueId(int id) {
        this.id = id;
    }

    public String getLeagueName() {
        return name;
    }

    public void setLeagueName(String name) {
        this.name = name;
    }

    public String getLeagueOwner() {
        return owner;
    }

    public void setLeagueOwner(String owner) {
        this.owner = owner;
    }

    public List<TeamBean> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamBean> teams) {
        this.teams = teams;
    }

    public String validate() {

        if (name == null || name.isEmpty()) {
            return "League Name is required";
        }
        return "";
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }
}
