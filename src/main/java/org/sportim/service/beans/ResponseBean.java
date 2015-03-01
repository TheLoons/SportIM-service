package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.soccer.beans.*;

import java.util.List;

/**
 * Catch-all response bean - includes status
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class ResponseBean {
    private StatusBean status;
    private List<EventBean> events;
    private EventBean event;
    private UserBean user;
    private List<UserBean> users;
    private TournamentBean tournament;
    private List<TeamBean> teams;
    private TeamBean team;
    private LeagueBean league;
    private List<LeagueBean> leagues;
    private int id = -1;
    private List<Integer> ids;
    private String token;
    private String session;
    private AggregateEventBean eventStats;
    private PlayerStatsBean playerStats;
    private TeamStatsBean teamStats;
    private PlayerPassingBean playerPasses;
    private TeamPassingBean teamPasses;
    private List<TeamPassingBean> eventPasses;
    private List<TeamResultsBean> tournamentResults;
    private List<TournamentBean> tables;

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

    public List<TeamBean> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamBean> teams) {
        this.teams = teams;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public AggregateEventBean getEventStats() {
        return eventStats;
    }

    public void setEventStats(AggregateEventBean eventStats) {
        this.eventStats = eventStats;
    }

    public PlayerStatsBean getPlayerStats() {
        return playerStats;
    }

    public void setPlayerStats(PlayerStatsBean playerStats) {
        this.playerStats = playerStats;
    }

    public TeamStatsBean getTeamStats() {
        return teamStats;
    }

    public void setTeamStats(TeamStatsBean teamStats) {
        this.teamStats = teamStats;
    }

    public List<LeagueBean> getLeagues() {
        return leagues;
    }

    public void setLeagues(List<LeagueBean> leagues) {
        this.leagues = leagues;
    }

    public List<UserBean> getUsers() {
        return users;
    }

    public void setUsers(List<UserBean> users) {
        this.users = users;
    }

    public PlayerPassingBean getPlayerPasses() {
        return playerPasses;
    }

    public void setPlayerPasses(PlayerPassingBean playerPasses) {
        this.playerPasses = playerPasses;
    }

    public TeamPassingBean getTeamPasses() {
        return teamPasses;
    }

    public void setTeamPasses(TeamPassingBean teamPasses) {
        this.teamPasses = teamPasses;
    }

    public List<TeamPassingBean> getEventPasses() {
        return eventPasses;
    }

    public void setEventPasses(List<TeamPassingBean> eventPasses) {
        this.eventPasses = eventPasses;
    }

    public List<TournamentBean> getTables() {
        return tables;
    }

    public void setTables(List<TournamentBean> tables) {
        this.tables = tables;
    }

    public List<TeamResultsBean> getTournamentResults() {
        return tournamentResults;
    }

    public void setTournamentResults(List<TeamResultsBean> tournamentResults) {
        this.tournamentResults = tournamentResults;
    }
}
