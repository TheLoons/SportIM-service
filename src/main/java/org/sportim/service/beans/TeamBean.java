package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by hannah on 12/4/14.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class TeamBean {
    private int id = 0;
    private String name;
    private String owner;

    public TeamBean(){
    }

    public TeamBean(ResultSet rs) throws SQLException {
        id = rs.getInt("t1.TeamId");
        name = rs.getString(2);
        owner = rs.getString(3);
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

    public String validate() {

        if (name == null || name.isEmpty()) {
            return "Team Name is required";
        }
        return "";
    }
}
