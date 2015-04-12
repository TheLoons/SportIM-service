package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Doug on 4/9/15.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ColorBean {
    private int id = 0;
    private String primaryColor;
    private String secondaryColor;
    private String tertiaryColor;

    public ColorBean(){
    }

    public ColorBean(ResultSet rs) throws SQLException {
        id = rs.getInt("t1.TeamId");
        primaryColor = rs.getString(2);
        secondaryColor = rs.getString(3);
        tertiaryColor = rs.getString(4);
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrimaryColor() {return this.primaryColor;}

    public void setPrimaryColor(String color) {this.primaryColor = color;}

    public String getSecondaryColor() {return this.secondaryColor;}

    public void setSecondaryColor(String color) {this.secondaryColor = color;}

    public String getTertiaryColor() {return this.tertiaryColor;}

    public void setTertiaryColor(String color) {this.tertiaryColor = color;}

    public String validate() {

        if (id < 1 || id == 0) {
            return "Team Id is required";
        }
        return "";
    }
}
