package org.sportim.service.soccer;

import org.apache.log4j.Logger;
import org.sportim.service.api.AggregationAPI;
import org.sportim.service.beans.ResponseBean;
import org.sportim.service.beans.stats.AggregateEventBean;
import org.sportim.service.beans.stats.LeagueStatsBean;
import org.sportim.service.beans.stats.PlayerStatsBean;
import org.sportim.service.beans.stats.TeamStatsBean;
import org.sportim.service.soccer.beans.SoccerEventBean;
import org.sportim.service.soccer.beans.SoccerLeagueStatsBean;
import org.sportim.service.soccer.beans.SoccerPlayerStatsBean;
import org.sportim.service.soccer.beans.SoccerTeamStatsBean;
import org.sportim.service.util.*;

import javax.ws.rs.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * The statistics aggregation API for soccer.
 *
 * See {@link org.sportim.service.api.AggregationAPI} for additional info.
 */
@Path("stats")
public class SoccerAggregationAPI implements AggregationAPI {
    private static Logger logger = Logger.getLogger(SoccerAggregationAPI.class.getName());
    private ConnectionProvider provider;

    public SoccerAggregationAPI() {
        provider = ConnectionManager.getInstance();
    }

    public SoccerAggregationAPI(ConnectionProvider provider) {
        this.provider = provider;
    }

    /**
     * Direct REST call for deleting event statistics.
     *
     * See {@link org.sportim.service.api.AggregationAPI#deleteEventStats} for more info.
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @return a ResponseBean containing the status
     */
    @DELETE
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean deleteEventStatsRest(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventTracking(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        if (deleteEventStats(eventID)) {
            return new ResponseBean(200, "");
        }
        return new ResponseBean(500, "Unable to delete event statistics");
    }

    public boolean deleteEventStats(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("DELETE FROM SoccerStats WHERE eventID = ?");
            stmt.setInt(1, eventID);
            stmt.executeUpdate();
            APIUtils.closeResources(stmt);
            stmt = conn.prepareStatement("DELETE FROM Passing WHERE eventID = ?");
            stmt.setInt(1, eventID);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.error("Unable to delete soccer stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return false;
        } finally {
            APIUtils.closeResources(stmt, conn);
        }
        return true;
    }

    /**
     * Direct REST call for getting event statistics.
     *
     * See {@link org.sportim.service.api.AggregationAPI#getEventStats} for more info.
     * @param eventID path param, the event ID
     * @param token header param, the user's authentication token
     * @return a ResponseBean containing the status and event results
     */
    @GET
    @Produces("application/json")
    @Path("event/{eventID}")
    public ResponseBean getEventStatsRest(@PathParam("eventID") final int eventID, @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasEventView(token, eventID)) {
            return new ResponseBean(401, "Not authorized");
        }

        AggregateEventBean eventStats = getEventStats(eventID);
        if (eventStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            resp.setEventStats(eventStats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    public AggregateEventBean getEventStats(int eventID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SoccerEventBean eventStats = new SoccerEventBean(eventID);
        boolean success = false;
        try {
            conn = provider.getConnection();
            stmt = conn.prepareStatement("SELECT teamID, SUM(goals), SUM(shots), SUM(shotsongoal), SUM(goalsagainst), " +
                    "SUM(fouls), SUM(yellow), SUM(red), SUM(saves) FROM SoccerStats " +
                    "WHERE eventID = ? " +
                    "GROUP BY teamID");
            stmt.setInt(1, eventID);
            rs = stmt.executeQuery();
            eventStats.teamStats = new ArrayList<TeamStatsBean>(2);
            while(rs.next()) {
                SoccerTeamStatsBean teamStats = new SoccerTeamStatsBean(rs.getInt(1));
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
                    SoccerPlayerStatsBean playerStats = new SoccerPlayerStatsBean(rs.getString(1));
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
            logger.error("Unable to get event soccer stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        if (success) {
            return eventStats;
        }
        return null;
    }

    /**
     * Direct REST call for getting player statistics.
     *
     * See {@link org.sportim.service.api.AggregationAPI#getPlayerStats} for more info.
     * @param login query param, the player's login
     * @param token header param, the user's authentication token
     * @param teamID query param, the team ID
     * @return a ResponseBean containing the status and player results
     */
    @GET
    @Produces("application/json")
    @Path("player")
    public ResponseBean getPlayerStatsRest(@QueryParam("login") final String login, @QueryParam("teamID") final int teamID,
                                       @HeaderParam("token") final String token) {
        if (!PrivilegeUtil.hasUserView(token, login)) {
            return new ResponseBean(401, "Not authorized");
        }

        PlayerStatsBean playerStats = getPlayerStats(login, teamID);

        if (playerStats != null) {
            ResponseBean resp = new ResponseBean(200, "");
            List<PlayerStatsBean> stats = new ArrayList<PlayerStatsBean>(1);
            stats.add(playerStats);
            resp.setPlayerStats(stats);
            return resp;
        }
        return new ResponseBean(500, "Unable to retrieve statistics.");
    }

    public PlayerStatsBean getPlayerStats(String login, int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SoccerPlayerStatsBean playerStats = null;
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
                playerStats = new SoccerPlayerStatsBean(login);
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
        } catch (Exception e) {
            logger.error("Unable to get player soccer stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            playerStats = null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }
        return playerStats;
    }

    /**
     * Direct REST call for getting team statistics.
     *
     * See {@link org.sportim.service.api.AggregationAPI#getTeamStats} for more info.
     * @param teamID path param, the team ID
     * @param token header param, the user's authentication token
     * @return a ResponseBean containing the status and team results
     */
    @GET
    @Produces("application/json")
    @Path("team/{teamID}")
    public ResponseBean getTeamsStatsRest(@PathParam("teamID") final int teamID, @HeaderParam("token") final String token) {
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

    public TeamStatsBean getTeamStats(int teamID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        SoccerTeamStatsBean teamStats = new SoccerTeamStatsBean(teamID);
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
            logger.error("Unable to get team soccer stats: " + e.getMessage());
            logger.debug(APIUtils.getStacktraceAsString(e));
            return null;
        } finally {
            APIUtils.closeResources(rs, stmt, conn);
        }

        return teamStats;
    }

    /**
     * Direct REST call for getting league statistics.
     *
     * See {@link org.sportim.service.api.AggregationAPI#getPlayerStats} for more info.
     * @param leagueID path param, the league ID
     * @param token header param, the user's authentication token
     * @return a ResponseBean containing the status and league results
     */
    @GET
    @Produces("application/json")
    @Path("league/{leagueID}")
    public ResponseBean getLeagueStatsRest(@PathParam("leagueID") final int leagueID, @HeaderParam("token") final String token) {
        if (AuthenticationUtil.validateToken(token) == null) {
            return new ResponseBean(401, "Not authorized");
        }

        LeagueStatsBean leagueStats = getLeagueStats(leagueID);
        if (leagueStats == null) {
            return new ResponseBean(500, "Unable to retrieve league stats.");
        }

        ResponseBean resp = new ResponseBean(200, "");
        resp.setLeagueStats(leagueStats);
        return resp;
    }

    public LeagueStatsBean getLeagueStats(int leagueID) {
        List<Integer> teams = StatUtil.getAllTeamsInLeague(leagueID);
        if (teams == null) {
            return null;
        }

        SoccerLeagueStatsBean leagueStats = new SoccerLeagueStatsBean(leagueID);
        leagueStats.teamStats = new ArrayList<TeamStatsBean>(teams.size());
        for (Integer teamID : teams) {
            TeamStatsBean teamStatsGen = getTeamStats(teamID);
            if (teamStatsGen != null) {
                SoccerTeamStatsBean teamStats = (SoccerTeamStatsBean)teamStatsGen;
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
        return leagueStats;
    }

    @Override
    public int getEventWinner(int eventID, Set<Integer> losers) {
        AggregateEventBean event = getEventStats(eventID);
        if (event == null || !(event instanceof SoccerEventBean)) {
            return -1;
        }

        SoccerEventBean eventStats = (SoccerEventBean)event;
        // Remove any non-winning teams from the next event, and add the winning team
        int winner = -1;
        int maxScore = -1;
        losers.clear();
        for (TeamStatsBean teamGen : eventStats.teamStats) {
            SoccerTeamStatsBean team = (SoccerTeamStatsBean)teamGen;
            if (team.goals > maxScore) {
                if (winner != -1) {
                    losers.add(winner);
                }
                winner = team.teamID;
                maxScore = team.goals;
            } else {
                losers.add(team.teamID);
            }
        }
        return winner;
    }
}
