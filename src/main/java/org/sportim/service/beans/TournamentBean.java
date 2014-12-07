package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.APIUtils;

import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
/**
 * Created by Doug on 12/7/14.
 */

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
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
