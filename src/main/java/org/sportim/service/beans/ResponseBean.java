package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Created by hannah on 12/4/14.
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ResponseBean {
    private StatusBean status;
    private List<EventBean> events;
    private EventBean event;
    private UserBean user;
    private TournamentBean tournament;
    private TeamBean team;
    private LeagueBean league;
    private int id = -1;
    private List<Integer> ids;

    public ResponseBean(){
    }

    public ResponseBean(int status, String message) {
        this.status = new StatusBean();
        this.status.setCode(status);
        this.status.setMessage(message);
    }

    public StatusBean getStatus() {
        return status;
    }

    public void setStatus(StatusBean status) {
        this.status = status;
    }

    public List<EventBean> getEvents() {
        return events;
    }

    public void setEvents(List<EventBean> events) {
        this.events = events;
    }

    public EventBean getEvent() {
        return event;
    }

    public void setEvent(EventBean event) {
        this.event = event;
    }

    public UserBean getUser() {
        return user;
    }

    public void setUser(UserBean user) {
        this.user = user;
    }

    public TournamentBean getTournament() {return tournament;}

    public void setTournament(TournamentBean tournament) {
        this.tournament = tournament;
    }

    public TeamBean getTeam() {return team;}

    public void setLeague(LeagueBean league) {
        this.league = league;
    }

    public LeagueBean getLeague() {return league;}

    public void setTeam(TeamBean team) {this.team = team;}

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_DEFAULT)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
}
