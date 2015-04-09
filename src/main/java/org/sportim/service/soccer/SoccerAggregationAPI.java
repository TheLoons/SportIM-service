package org.sportim.service.soccer;

import org.sportim.service.beans.ResponseBean;
import org.sportim.service.soccer.beans.AggregateEventBean;
import org.sportim.service.soccer.beans.LeagueStatsBean;
import org.sportim.service.soccer.beans.PlayerStatsBean;
import org.sportim.service.soccer.beans.TeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * API for getting aggregated stats
 */
@Path("stats")
public class SoccerAggregationAPI {
    private ConnectionProvider provider;

    public SoccerAggregationAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerAggregationAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    @GET
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean getEventStats(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventView(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregateEventBean eventStats = getEventStats(eventID, provider);
        if (eventStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setEventStats(eventStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    protected static AggregateEventBean getEventStats(int eventID, ConnectionProvider connProvider) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        AggregateEventBean eventStats = new AggregateEventBean();
        boolean success = false;
        try {
            conn = connProvider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                    "SUM(fouls), SUM(yellow), SUM(red), SUM(saves) FROM SoccerStats " +
                    "WHERE eventID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            eventStats.teamStats = new ArrayList<TeamStatsBean>(2);
            while(rs.next()) {
                TeamStatsBean teamStats = new TeamStatsBean();
                teamStats.teamID = rs.getInt(1);
                teamStats.goals = rs.getInt(2);
                teamStats.shots = rs.getInt(3);
                teamStats.shotsOnGoal = rs.getInt(4);
                teamStats.goalsAgainst = rs.getInt(5);
                teamStats.fouls = rs.getInt(6);
                teamStats.yellow = rs.getInt(7);
                teamStats.red = rs.getInt(8);
                teamStats.saves = rs.getInt(9);
                eventStats.teamStats.add(teamStats);

                eventStats.totalGoals += teamStats.goals;
                eventStats.totalShots += teamStats.shots;
                eventStats.totalShotsOnGoal += teamStats.shotsOnGoal;
            }

            APIUtils.closeResources(rs, stmt);
            for (TeamStatsBean team : eventStats.teamStats) {
                stmt = conn.prepareStatement("SELECT player, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red), SUM(assists), SUM(minutes), SUM(saves) FROM SoccerStats " +
                        "WHERE eventID = ? AND teamID = ? " +
                        "GROUP BY player");
                stmt.setInt(1, eventID);
                stmt.setInt(2, team.teamID);
                rs = stmt.executeQuery();
                team.playerStats = new ArrayList<PlayerStatsBean>();
                while(rs.next()) {
                    PlayerStatsBean playerStats = new PlayerStatsBean();
                    playerStats.login = rs.getString(1);
                    playerStats.goals = rs.getInt(2);
                    playerStats.shots = rs.getInt(3);
                    playerStats.shotsOnGoal = rs.getInt(4);
                    playerStats.goalsAgainst = rs.getInt(5);
                    playerStats.fouls = rs.getInt(6);
                    playerStats.yellow = rs.getInt(7);
                    playerStats.red = rs.getInt(8);
                    playerStats.assists = rs.getInt(9);
                    playerStats.minutes = rs.getInt(10);
                    playerStats.saves = rs.getInt(11);
                    team.playerStats.add(playerStats);
                }
                APIUtils.closeResources(rs, stmt);
            }
            success = true;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return eventStats;
        }
        return null;
    }

    @GET
    @Produces("application/json")
    @Path("player")
    public ResponseBean getPlayerStats(@QueryParam("login") final String login, @QueryParam("teamID") final int teamID,
                                       @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        PlayerStatsBean playerStats = new PlayerStatsBean();
        boolean success = false;
        try {
            conn = provider.getConnection();
            if (teamID < 1) {
                stmt = conn.prepareStatement("SELECT player, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red), SUM(assists), SUM(saves), SUM(minutes) FROM SoccerStats " +
                        "WHERE player = ?" +
                        "GROUP BY player");
                stmt.setString(1, login);
            } else {
                stmt = conn.prepareStatement("SELECT player, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                        "SUM(fouls), SUM(yellow), SUM(red), SUM(assists), SUM(saves), SUM(minutes) FROM SoccerStats " +
                        "WHERE player = ? AND teamID = ? " +
                        "GROUP BY player");
                stmt.setString(1, login);
                stmt.setInt(2, teamID);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                playerStats.login = rs.getString(1);
                playerStats.goals = rs.getInt(2);
                playerStats.shots = rs.getInt(3);
                playerStats.shotsOnGoal = rs.getInt(4);
                playerStats.goalsAgainst = rs.getInt(5);
                playerStats.fouls = rs.getInt(6);
                playerStats.yellow = rs.getInt(7);
                playerStats.red = rs.getInt(8);
                playerStats.assists = rs.getInt(9);
                playerStats.saves = rs.getInt(10);
                playerStats.minutes = rs.getInt(11);
            }
            success = true;
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setPlayerStats(playerStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    @GET
    @Produces("application/json")
    @Path("team/{teamID}")
    public ResponseBean getTeamsStats(@PathParam("teamID") final int teamID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        TeamStatsBean teamStats = getTeamStats(teamID);
        if (teamStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setTeamStats(teamStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    private TeamStatsBean getTeamStats(int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        TeamStatsBean teamStats = new TeamStatsBean();
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                    "SUM(fouls), SUM(yellow), SUM(red), SUM(saves) FROM SoccerStats " +
                    "WHERE teamID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, teamID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                teamStats.teamID = rs.getInt(1);
                teamStats.goals = rs.getInt(2);
                teamStats.shots = rs.getInt(3);
                teamStats.shotsOnGoal = rs.getInt(4);
                teamStats.goalsAgainst = rs.getInt(5);
                teamStats.fouls = rs.getInt(6);
                teamStats.yellow = rs.getInt(7);
                teamStats.red = rs.getInt(8);
                teamStats.saves = rs.getInt(9);
            }
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
            return null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return teamStats;
    }

    @GET
    @Produces("application/json")
    @Path("league/{leagueID}")
    public ResponseBean getLeagueStats(@PathParam("leagueID") final int leagueID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        List<Integer> teams = getAllTeamsInLeague(leagueID);
        if (teams == null) {
            return new ResponseBean(500, "Unable to retrieve league stats.");
        }

        LeagueStatsBean leagueStats = new LeagueStatsBean(leagueID);
        leagueStats.teamStats = new ArrayList<TeamStatsBean>(teams.size());
        for (Integer teamID : teams) {
            TeamStatsBean teamStats = getTeamStats(teamID);
            if (teamStats != null) {
                leagueStats.teamStats.add(teamStats);
                if (teamStats.goals > leagueStats.topTeamScore) {
                    leagueStats.topScoringTeam = teamID;
                    leagueStats.topTeamScore = teamStats.goals;
                }
                leagueStats.goals += teamStats.goals;
                leagueStats.fouls += teamStats.fouls;
                leagueStats.yellow += teamStats.yellow;
                leagueStats.red += teamStats.red;
                leagueStats.shots += teamStats.shots;
                leagueStats.shotsOnGoal += teamStats.shotsOnGoal;
            }
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setLeagueStats(leagueStats);
        return resp;
    }

    private List<Integer> getAllTeamsInLeague(int leagueID) {
        List<Integer> teams = new LinkedList<Integer>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT TeamId FROM TeamBelongsTo WHERE LeagueId = ?");
            stmt.setInt(1, leagueID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                teams.add(rs.getInt(1));
            }
        } catch (Exception e) {
            return null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return teams;
    }
}
