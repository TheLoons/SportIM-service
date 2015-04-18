package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.SportType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by hannah on 12/4/14.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TeamBean {
    private int id = 0;
    private String name;
    private String owner;
    private List<UserBean> players;
    private SportType sport;

    public TeamBean(){
    }

    public TeamBean(ResultSet rs) throws SQLException {
        id = rs.getInt("t1.TeamId");
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

    public List<UserBean> getPlayers() {return this.players;}

    public void setPlayers(List<UserBean> players) {this.players = players;}

    public String validate() {

        if (name == null || name.isEmpty()) {
            return "Team Name is required";
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
