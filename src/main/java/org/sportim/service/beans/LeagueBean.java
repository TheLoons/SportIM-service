package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.sportim.service.util.SportType;


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
    private SportType sport;

    public LeagueBean(){
    }

    public LeagueBean(ResultSet rs) throws SQLException {
        id = rs.getInt(1);
        name = rs.getString(2);
        owner = rs.getString(3);
        sport = SportType.fromString(rs.getString(4));
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
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

    public SportType getSport() {
        return sport;
    }

    public void setSport(SportType sport) {
        this.sport = sport;
    }
}
