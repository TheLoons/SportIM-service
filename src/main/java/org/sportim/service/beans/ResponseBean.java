package org.sportim.service.beans;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.PlayerPassingBean;
import org.sportim.service.beans.stats.TeamPassingBean;
import org.sportim.service.soccer.beans.*;

import java.util.List;
import java.util.SortedSet;

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
    private SoccerPlayerStatsBean playerStats;
    private SoccerTeamStatsBean teamStats;
    private SoccerLeagueStatsBean leagueStats;
    private PlayerPassingBean playerPasses;
    private TeamPassingBean teamPasses;
    private List<TeamPassingBean> eventPasses;
    private SortedSet<SoccerTeamResultsBean> tournamentResults;
    private List<TournamentBean> tables;
    private ColorBean colors;
    private List<String> sports;

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

    public ColorBean getColors() {return colors;}

    public void setColors(ColorBean colorBean) {this.colors = colorBean;}

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

    public SoccerPlayerStatsBean getPlayerStats() {
        return playerStats;
    }

    public void setPlayerStats(SoccerPlayerStatsBean playerStats) {
        this.playerStats = playerStats;
    }

    public SoccerTeamStatsBean getTeamStats() {
        return teamStats;
    }

    public void setTeamStats(SoccerTeamStatsBean teamStats) {
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

    public SortedSet<SoccerTeamResultsBean> getTournamentResults() {
        return tournamentResults;
    }

    public void setTournamentResults(SortedSet<SoccerTeamResultsBean> tournamentResults) {
        this.tournamentResults = tournamentResults;
    }

    public SoccerLeagueStatsBean getLeagueStats() {
        return leagueStats;
    }

    public void setLeagueStats(SoccerLeagueStatsBean leagueStats) {
        this.leagueStats = leagueStats;
    }

    public List<String> getSports() {
        return sports;
    }

    public void setSports(List<String> sports) {
        this.sports = sports;
    }
}
