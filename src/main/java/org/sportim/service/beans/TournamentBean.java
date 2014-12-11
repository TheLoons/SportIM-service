package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Doug on 12/7/14.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TournamentBean
{
    int tournamentId;
    String tournamentName;
    int leagueId;
    String desc;

    public TournamentBean() {
    }

    public TournamentBean(ResultSet rs) throws SQLException {
        tournamentId = rs.getInt(1);
        tournamentName = rs.getString(2);
        leagueId = rs.getInt(3);
        desc = rs.getString(4);
    }


    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getTournamentID() {
        return tournamentId;
    }

    public void setTournamentID(int tournamentID) {
        this.tournamentId = tournamentID;
    }

    public String getDesc() {return desc;}

    public void setDesc(String desc) {this.desc = desc;}

    public int getLeagueId() {return leagueId;}

    public void setLeagueId(int leagueId) {this.leagueId = leagueId;}

    public String validate() {

        if (tournamentName == null || tournamentName.isEmpty()) {
            return "Tournament Name is required";
        }
        return "";
    }

}
