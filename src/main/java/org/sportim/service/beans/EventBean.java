package org.sportim.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.util.APIUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by hannah on 12/1/14.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class EventBean {
    private String title;
    private String owner;
    private String sport;
    private long start;
    private long end;
    private List<Integer> teamIDs;
    private List<String> playerIDs;
    private List<TeamBean> teams;
    private List<UserBean> players;
    private int tournamentID = 0;
    private int id;
    private int nextEventID = 0;
    private boolean editable = false;
    private String location = "";
    private String type = "";

    public EventBean() {
    }

    public EventBean(ResultSet rs) throws SQLException {
        title = rs.getString(1);
        start = rs.getLong(2);
        end = rs.getLong(3);
        tournamentID = rs.getInt(4);
        id = rs.getInt(5);
        owner = rs.getString(6);
        nextEventID = rs.getInt(7);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStart() {
        return APIUtils.millisToUTCString(start);
    }

    @JsonIgnore
    public long getStartMillis() {
        return start;
    }

    public void setStart(String start) {
        this.start = APIUtils.parseDateTime(start).getMillis();
    }

    public void setStartMillis(long millis) { this.start = millis; }

    public String getEnd() {
        return APIUtils.millisToUTCString(end);
    }

    @JsonIgnore
    public long getEndMillis() {
        return end;
    }

    public void setEnd(String end) {
        this.end = APIUtils.parseDateTime(end).getMillis();
    }

    public void setEndMillis(long millis) { this.end = millis; }

    public List<Integer> getTeamIDs() {
        return teamIDs;
    }

    public void setTeamIDs(List<Integer> teamIDs) {
        this.teamIDs = teamIDs;
    }

    public List<String> getPlayerIDs() {
        return playerIDs;
    }

    public void setPlayerIDs(List<String> playerIDs) {
        this.playerIDs = playerIDs;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getTournamentID() {
        return tournamentID;
    }

    public void setTournamentID(int tournamentID) {
        this.tournamentID = tournamentID;
    }

    public String validate() {
        if (title == null || title.isEmpty()) {
            return "Title is required";
        }
        return "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<TeamBean> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamBean> teams) {
        this.teams = teams;
    }

    public List<UserBean> getPlayers() {
        return players;
    }

    public void setPlayers(List<UserBean> players) {
        this.players = players;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getNextEventID() {
        return nextEventID;
    }

    public void setNextEventID(int nextEventID) {
        this.nextEventID = nextEventID;
    }

    public String getLocation() {return location;}

    public void setLocation(String location) {this.location = location;}

    public String getType() {return type;}

    public void setType(String type) {this.type = type;}

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }
}
