package org.sportim.service.beans;

import org.sportim.service.util.APIUtils;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by hannah on 12/1/14.
 */
public class EventBean {
    @NotNull
    private String title;
    private long start;
    private long end;
    private List<Integer> teamIDs;
    private List<String> playerIDs;
    private int tournamentID = -1;

    public EventBean() {
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

    public long getStartMillis() {
        return start;
    }

    public void setStart(String start) {
        this.start = APIUtils.parseDateTime(start).getMillis();
    }

    public String getEnd() {
        return APIUtils.millisToUTCString(end);
    }

    public long getEndMillis() {
        return end;
    }

    public void setEnd(String end) {
        this.end = APIUtils.parseDateTime(end).getMillis();
    }

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
}
